package com.petsalud.backendpetsalud.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Semana 6 (TDD, ciclo 1): los estados de una cita son un conjunto cerrado, no texto libre. */
class EstadoCitaTest {

    @Test
    @DisplayName("Los estados posibles son exactamente AGENDADA, CANCELADA y COMPLETADA")
    void estados_sonLosTresEsperados() {
        assertEquals(List.of(EstadoCita.AGENDADA, EstadoCita.CANCELADA, EstadoCita.COMPLETADA),
                List.of(EstadoCita.values()));
    }

    @ParameterizedTest
    @CsvSource({
            "Agendada,AGENDADA",
            "AGENDADA,AGENDADA",
            "cancelada,CANCELADA",
            "'  Completada  ',COMPLETADA"
    })
    @DisplayName("El texto se convierte en estado sin importar mayusculas ni espacios")
    void desdeTexto_sinImportarElFormato_devuelveElEstado(String texto, EstadoCita esperado) {
        assertEquals(esperado, EstadoCita.desdeTexto(texto));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "Pendiente", "Cancelado", "AGENDADAS"})
    @DisplayName("Un texto vacio o que no corresponde a un estado se rechaza")
    void desdeTexto_textoInvalido_lanzaError(String texto) {
        assertThrows(IllegalArgumentException.class, () -> EstadoCita.desdeTexto(texto));
    }
}
