package com.petsalud.backendpetsalud.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueo temporal del login por intentos fallidos (fuerza bruta).
 * Tras {@code login.max-attempts} fallos seguidos (5 por defecto) el email queda bloqueado
 * {@code login.lock-minutes} minutos (5 por defecto). Mientras dura el bloqueo ni siquiera
 * se evalua la contrasena, y el bloqueo no se extiende con nuevos intentos.
 * Un login correcto reinicia el contador. Los fallos antiguos (mas viejos que la duracion
 * del bloqueo) no se acumulan. El estado vive en memoria: se pierde al reiniciar la aplicacion.
 */
@Component
public class LoginAttemptService {

    private record Estado(int fallos, Instant ultimoFallo, Instant bloqueadoHasta) {
    }

    private static final int LIMITE_PURGA = 10_000;

    private final int maxIntentos;
    private final Duration duracionBloqueo;
    private final Clock clock;
    private final ConcurrentHashMap<String, Estado> intentos = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(@Value("${login.max-attempts:5}") int maxIntentos,
                               @Value("${login.lock-minutes:5}") long minutosBloqueo) {
        this(maxIntentos, Duration.ofMinutes(minutosBloqueo), Clock.systemUTC());
    }

    // Constructor para pruebas (reloj controlable)
    LoginAttemptService(int maxIntentos, Duration duracionBloqueo, Clock clock) {
        this.maxIntentos = maxIntentos;
        this.duracionBloqueo = duracionBloqueo;
        this.clock = clock;
    }

    /** Segundos que faltan para que termine el bloqueo; 0 si el email no esta bloqueado. */
    public long segundosRestantes(String email) {
        Estado e = intentos.get(clave(email));
        if (e == null || e.bloqueadoHasta() == null) {
            return 0;
        }
        long ms = Duration.between(clock.instant(), e.bloqueadoHasta()).toMillis();
        return ms <= 0 ? 0 : (ms + 999) / 1000;
    }

    public boolean estaBloqueado(String email) {
        return segundosRestantes(email) > 0;
    }

    /** Fallos seguidos registrados para el email (para el log de seguridad). */
    public int fallosRegistrados(String email) {
        Estado e = intentos.get(clave(email));
        return e == null ? 0 : e.fallos();
    }

    /** Duracion del bloqueo en minutos (para el log de seguridad). */
    public long minutosBloqueo() {
        return duracionBloqueo.toMinutes();
    }

    /** Registra un fallo de login. Devuelve true si este fallo provoco el bloqueo. */
    public boolean registrarFallo(String email) {
        Instant ahora = clock.instant();
        boolean[] bloqueado = {false};
        intentos.compute(clave(email), (k, e) -> {
            if (e != null && e.bloqueadoHasta() != null && ahora.isBefore(e.bloqueadoHasta())) {
                return e; // ya esta bloqueado: el bloqueo no se extiende
            }
            int fallos = 1;
            if (e != null && e.bloqueadoHasta() == null
                    && Duration.between(e.ultimoFallo(), ahora).compareTo(duracionBloqueo) < 0) {
                fallos = e.fallos() + 1;
            }
            if (fallos >= maxIntentos) {
                bloqueado[0] = true;
                return new Estado(fallos, ahora, ahora.plus(duracionBloqueo));
            }
            return new Estado(fallos, ahora, null);
        });
        purgar(ahora);
        return bloqueado[0];
    }

    /** Un login correcto borra el historial de fallos del email. */
    public void registrarExito(String email) {
        intentos.remove(clave(email));
    }

    private void purgar(Instant ahora) {
        if (intentos.size() < LIMITE_PURGA) {
            return;
        }
        intentos.entrySet().removeIf(en -> {
            Estado e = en.getValue();
            return e.bloqueadoHasta() != null
                    ? !ahora.isBefore(e.bloqueadoHasta())
                    : Duration.between(e.ultimoFallo(), ahora).compareTo(duracionBloqueo) >= 0;
        });
    }

    private String clave(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
