package com.petsalud.backendpetsalud.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    /** Reloj manual para avanzar el tiempo sin esperar. */
    static class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-01-01T10:00:00Z");

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    private static final String EMAIL = "cliente@petsalud.cl";

    private RelojManual reloj;
    private LoginAttemptService servicio;

    @BeforeEach
    void preparar() {
        reloj = new RelojManual();
        servicio = new LoginAttemptService(5, Duration.ofMinutes(5), reloj);
    }

    @Test
    void cuatroFallosNoBloquean() {
        for (int i = 0; i < 4; i++) {
            assertFalse(servicio.registrarFallo(EMAIL));
        }
        assertFalse(servicio.estaBloqueado(EMAIL));
    }

    @Test
    void quintoFalloBloqueaPorCincoMinutos() {
        for (int i = 0; i < 4; i++) {
            servicio.registrarFallo(EMAIL);
        }
        assertTrue(servicio.registrarFallo(EMAIL));
        assertTrue(servicio.estaBloqueado(EMAIL));
        assertEquals(300, servicio.segundosRestantes(EMAIL));
    }

    @Test
    void sigueBloqueadoAntesDeCincoMinutosYSeDesbloqueaDespues() {
        for (int i = 0; i < 5; i++) {
            servicio.registrarFallo(EMAIL);
        }
        reloj.avanzar(Duration.ofMinutes(4).plusSeconds(59));
        assertTrue(servicio.estaBloqueado(EMAIL));
        reloj.avanzar(Duration.ofSeconds(1));
        assertFalse(servicio.estaBloqueado(EMAIL));
        assertEquals(0, servicio.segundosRestantes(EMAIL));
    }

    @Test
    void despuesDelBloqueoElContadorEmpiezaDeCero() {
        for (int i = 0; i < 5; i++) {
            servicio.registrarFallo(EMAIL);
        }
        reloj.avanzar(Duration.ofMinutes(5));
        for (int i = 0; i < 4; i++) {
            assertFalse(servicio.registrarFallo(EMAIL));
        }
        assertFalse(servicio.estaBloqueado(EMAIL));
        assertTrue(servicio.registrarFallo(EMAIL));
    }

    @Test
    void nuevosIntentosNoExtiendenElBloqueo() {
        for (int i = 0; i < 5; i++) {
            servicio.registrarFallo(EMAIL);
        }
        reloj.avanzar(Duration.ofMinutes(2));
        assertFalse(servicio.registrarFallo(EMAIL));
        assertEquals(180, servicio.segundosRestantes(EMAIL));
    }

    @Test
    void loginCorrectoReiniciaElContador() {
        for (int i = 0; i < 4; i++) {
            servicio.registrarFallo(EMAIL);
        }
        servicio.registrarExito(EMAIL);
        for (int i = 0; i < 4; i++) {
            assertFalse(servicio.registrarFallo(EMAIL));
        }
        assertFalse(servicio.estaBloqueado(EMAIL));
    }

    @Test
    void fallosAntiguosNoSeAcumulan() {
        for (int i = 0; i < 4; i++) {
            servicio.registrarFallo(EMAIL);
        }
        reloj.avanzar(Duration.ofMinutes(6));
        assertFalse(servicio.registrarFallo(EMAIL));
        assertFalse(servicio.estaBloqueado(EMAIL));
    }

    @Test
    void elBloqueoIgnoraMayusculasYEspaciosYEsPorUsuario() {
        for (int i = 0; i < 5; i++) {
            servicio.registrarFallo("  CLIENTE@Petsalud.cl ");
        }
        assertTrue(servicio.estaBloqueado(EMAIL));
        assertFalse(servicio.estaBloqueado("vet@petsalud.cl"));
    }
}
