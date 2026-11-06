package com.petsalud.backendpetsalud.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro y monitoreo basico de actividad sospechosa (Semana 3).
 *
 * Escribe en el logger "SEGURIDAD" un evento por linea, con fecha y hora (las agrega Logback),
 * IP de origen, usuario, metodo HTTP y ruta. Con {@code logging.file.name} los eventos quedan
 * ademas en un archivo, que es lo que revisaria un IDS/IPS o un SIEM.
 *
 * Eventos: LOGIN_OK, LOGIN_FALLIDO, CUENTA_BLOQUEADA, LOGIN_RECHAZADO_BLOQUEO, JWT_INVALIDO,
 * SIN_AUTENTICACION (401), ACCESO_DENEGADO (403), ENTRADA_RECHAZADA (400 por validacion),
 * OPERACION (escrituras exitosas) y RAFAGA_SOLICITUDES (muchas solicitudes desde una IP).
 *
 * Nunca registra contrasenas ni tokens. Los valores que vienen del cliente se limpian de saltos
 * de linea para evitar que alguien falsifique lineas del log (log injection, CWE-117).
 */
@Service
public class ActividadSospechosaService {

    private static final Logger log = LoggerFactory.getLogger("SEGURIDAD");
    private static final int LARGO_MAXIMO = 100;
    private static final int LIMITE_PURGA = 10_000;

    private final int maxSolicitudes;
    private final Duration ventana;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Instant>> solicitudesPorIp = new ConcurrentHashMap<>();

    @Autowired
    public ActividadSospechosaService(@Value("${seguridad.monitor.max-solicitudes:200}") int maxSolicitudes,
                                      @Value("${seguridad.monitor.ventana-minutos:15}") long ventanaMinutos) {
        this(maxSolicitudes, Duration.ofMinutes(ventanaMinutos), Clock.systemUTC());
    }

    // Constructor para pruebas (reloj controlable)
    ActividadSospechosaService(int maxSolicitudes, Duration ventana, Clock clock) {
        this.maxSolicitudes = maxSolicitudes;
        this.ventana = ventana;
        this.clock = clock;
    }

    public void loginExitoso(HttpServletRequest req, String email) {
        log.info("LOGIN_OK usuario={} ip={}", limpiar(email), ip(req));
    }

    public void loginFallido(HttpServletRequest req, String email, int fallosSeguidos) {
        log.warn("LOGIN_FALLIDO usuario={} ip={} fallosSeguidos={}", limpiar(email), ip(req), fallosSeguidos);
    }

    public void cuentaBloqueada(HttpServletRequest req, String email, long minutos) {
        log.warn("CUENTA_BLOQUEADA usuario={} ip={} minutos={} motivo=posible_fuerza_bruta",
                limpiar(email), ip(req), minutos);
    }

    public void loginDuranteBloqueo(HttpServletRequest req, String email) {
        log.warn("LOGIN_RECHAZADO_BLOQUEO usuario={} ip={}", limpiar(email), ip(req));
    }

    /** Token alterado, vencido, de otro emisor/audiencia o de un usuario que ya no existe. */
    public void jwtInvalido(HttpServletRequest req, String motivo) {
        log.warn("JWT_INVALIDO ip={} {} {} motivo={}", ip(req), req.getMethod(), ruta(req), limpiar(motivo));
    }

    /** Recurso protegido sin token valido: 401. */
    public void sinAutenticacion(HttpServletRequest req) {
        log.warn("SIN_AUTENTICACION ip={} {} {}", ip(req), req.getMethod(), ruta(req));
    }

    /** Usuario autenticado sin permiso (rol o propiedad del recurso): 403. */
    public void accesoDenegado(HttpServletRequest req, String usuario) {
        log.warn("ACCESO_DENEGADO usuario={} ip={} {} {}", limpiar(usuario), ip(req), req.getMethod(), ruta(req));
    }

    /** Datos que no pasaron la validacion (posible intento de XSS o SQL Injection). Solo se registran los campos. */
    public void entradaRechazada(HttpServletRequest req, String usuario, Collection<String> campos) {
        log.warn("ENTRADA_RECHAZADA usuario={} ip={} {} {} campos={}",
                limpiar(usuario), ip(req), req.getMethod(), ruta(req), limpiar(String.join(",", campos)));
    }

    /** Trazabilidad de escrituras exitosas: quien creo, modifico o elimino que cosa. */
    public void operacion(HttpServletRequest req, String usuario, int status) {
        log.info("OPERACION usuario={} ip={} {} {} status={}", limpiar(usuario), ip(req), req.getMethod(), ruta(req), status);
    }

    /**
     * Cuenta las solicitudes por IP dentro de la ventana. Al superar el maximo se registra
     * RAFAGA_SOLICITUDES (posible fuerza bruta o denegacion de servicio); luego se repite
     * cada 50 solicitudes para no llenar el log.
     */
    public int registrarSolicitud(HttpServletRequest req) {
        String ip = ip(req);
        Instant ahora = clock.instant();
        Deque<Instant> marcas = solicitudesPorIp.computeIfAbsent(ip, k -> new ArrayDeque<>());
        int total;
        synchronized (marcas) {
            marcas.addLast(ahora);
            Instant corte = ahora.minus(ventana);
            while (!marcas.isEmpty() && marcas.peekFirst().isBefore(corte)) {
                marcas.pollFirst();
            }
            total = marcas.size();
        }
        if (total > maxSolicitudes && (total - maxSolicitudes - 1) % 50 == 0) {
            log.warn("RAFAGA_SOLICITUDES ip={} solicitudes={} ventanaMinutos={}", ip, total, ventana.toMinutes());
        }
        purgar(ahora);
        return total;
    }

    private void purgar(Instant ahora) {
        if (solicitudesPorIp.size() < LIMITE_PURGA) {
            return;
        }
        Instant corte = ahora.minus(ventana);
        solicitudesPorIp.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                Instant ultima = e.getValue().peekLast();
                return ultima == null || ultima.isBefore(corte);
            }
        });
    }

    // Se usa la IP de la conexion. Si la API queda detras de un proxy, configurar
    // server.forward-headers-strategy en vez de confiar en X-Forwarded-For (lo puede falsificar el cliente).
    private static String ip(HttpServletRequest req) {
        return limpiar(req.getRemoteAddr());
    }

    private static String ruta(HttpServletRequest req) {
        return limpiar(req.getRequestURI());
    }

    static String limpiar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }
        String limpio = valor.replaceAll("[\\r\\n\\t]", "_");
        return limpio.length() > LARGO_MAXIMO ? limpio.substring(0, LARGO_MAXIMO) + "..." : limpio;
    }
}
