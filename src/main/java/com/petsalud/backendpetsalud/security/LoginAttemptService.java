package com.petsalud.backendpetsalud.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mitigación de fuerza bruta: tras N intentos fallidos la cuenta queda bloqueada temporalmente.
 * Implementación en memoria (suficiente para una instancia; con varias instancias se movería a Redis o BD).
 */
@Service
public class LoginAttemptService {

    private final int maxIntentos;
    private final Duration duracionBloqueo;
    private final Map<String, Registro> intentos = new ConcurrentHashMap<>();

    public LoginAttemptService(@Value("${petsalud.seguridad.max-intentos-fallidos:5}") int maxIntentos,
                               @Value("${petsalud.seguridad.minutos-bloqueo:15}") long minutosBloqueo) {
        this.maxIntentos = maxIntentos;
        this.duracionBloqueo = Duration.ofMinutes(minutosBloqueo);
    }

    /** Registra un fallo y devuelve true si con este fallo la cuenta quedó bloqueada. */
    public boolean registrarFallo(String usuario) {
        Registro r = intentos.compute(clave(usuario), (k, actual) -> {
            Registro nuevo = (actual == null || actual.expirado()) ? new Registro() : actual;
            nuevo.fallos++;
            if (nuevo.fallos >= maxIntentos && nuevo.bloqueadoHasta == null) {
                nuevo.bloqueadoHasta = Instant.now().plus(duracionBloqueo);
            }
            return nuevo;
        });
        return r.fallos == maxIntentos;
    }

    public void registrarExito(String usuario) {
        intentos.remove(clave(usuario));
    }

    public boolean estaBloqueado(String usuario) {
        Registro r = intentos.get(clave(usuario));
        if (r == null || r.bloqueadoHasta == null) {
            return false;
        }
        if (r.expirado()) {
            intentos.remove(clave(usuario));
            return false;
        }
        return true;
    }

    private String clave(String usuario) {
        return usuario == null ? "" : usuario.trim().toLowerCase();
    }

    private static final class Registro {
        int fallos;
        Instant bloqueadoHasta;

        boolean expirado() {
            return bloqueadoHasta != null && Instant.now().isAfter(bloqueadoHasta);
        }
    }
}
