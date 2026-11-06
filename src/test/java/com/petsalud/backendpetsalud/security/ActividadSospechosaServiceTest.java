package com.petsalud.backendpetsalud.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(OutputCaptureExtension.class)
class ActividadSospechosaServiceTest {

    private MockHttpServletRequest solicitud(String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr(ip);
        return req;
    }

    @Test
    void superarElMaximoDeSolicitudesRegistraRafaga(CapturedOutput salida) {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-06T15:00:00Z"), ZoneOffset.UTC);
        ActividadSospechosaService actividad = new ActividadSospechosaService(3, Duration.ofMinutes(15), reloj);

        for (int i = 0; i < 3; i++) {
            actividad.registrarSolicitud(solicitud("10.0.0.9"));
        }
        assertThat(salida.getOut()).doesNotContain("RAFAGA_SOLICITUDES");

        assertEquals(4, actividad.registrarSolicitud(solicitud("10.0.0.9")));
        assertThat(salida.getOut()).contains("RAFAGA_SOLICITUDES ip=10.0.0.9 solicitudes=4 ventanaMinutos=15");
    }

    /** Reloj que la prueba puede adelantar. */
    private static final class RelojMovible extends Clock {
        private Instant ahora;
        RelojMovible(Instant inicio) { this.ahora = inicio; }
        void avanzar(Duration d) { ahora = ahora.plus(d); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zona) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    @Test
    void lasSolicitudesFueraDeLaVentanaNoSeAcumulan() {
        RelojMovible reloj = new RelojMovible(Instant.parse("2026-11-06T15:00:00Z"));
        ActividadSospechosaService actividad = new ActividadSospechosaService(3, Duration.ofMinutes(15), reloj);

        actividad.registrarSolicitud(solicitud("10.0.0.7"));
        actividad.registrarSolicitud(solicitud("10.0.0.7"));
        reloj.avanzar(Duration.ofMinutes(16));

        assertEquals(1, actividad.registrarSolicitud(solicitud("10.0.0.7")));
    }

    @Test
    void losSaltosDeLineaNoPermitenFalsificarLineasDelLog(CapturedOutput salida) {
        ActividadSospechosaService actividad = new ActividadSospechosaService(200, 15);
        actividad.loginFallido(solicitud("10.0.0.5"), "x@petsalud.cl\nLOGIN_OK usuario=admin@petsalud.cl", 1);

        assertThat(salida.getOut())
                .contains("usuario=x@petsalud.cl_LOGIN_OK usuario=admin@petsalud.cl")
                .doesNotContain("\nLOGIN_OK usuario=admin@petsalud.cl");
    }

    @Test
    void entradaRechazadaRegistraSoloLosCampos(CapturedOutput salida) {
        ActividadSospechosaService actividad = new ActividadSospechosaService(200, 15);
        actividad.entradaRechazada(solicitud("10.0.0.6"), "cliente@petsalud.cl", List.of("nombre", "raza"));
        assertThat(salida.getOut()).contains("ENTRADA_RECHAZADA usuario=cliente@petsalud.cl ip=10.0.0.6 POST /api/auth/login campos=nombre,raza");
    }
}
