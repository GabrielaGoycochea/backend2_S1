package com.petsalud.backendpetsalud.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class EstadoCitaTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final EstadoCita.Conversor conversor = new EstadoCita.Conversor();

    @ParameterizedTest
    @CsvSource({"AGENDADA,Agendada", "CANCELADA,Cancelada", "COMPLETADA,Completada"})
    void conservaContratoJpaYJson(EstadoCita estado, String texto) throws Exception {
        assertEquals(texto, conversor.convertToDatabaseColumn(estado));
        assertEquals(estado, conversor.convertToEntityAttribute(texto));
        assertEquals(mapper.writeValueAsString(texto), mapper.writeValueAsString(estado));
        assertEquals(estado, mapper.readValue(mapper.writeValueAsString(texto), EstadoCita.class));
    }

    @Test
    void rechazaEstadoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> EstadoCita.desdeTexto("Inventado"));
        assertThrows(IllegalArgumentException.class, () -> conversor.convertToEntityAttribute("Inventado"));
    }

    @Test
    void reconoceEstadosSinDistinguirMayusculas() {
        assertEquals(EstadoCita.AGENDADA, EstadoCita.desdeTexto("agendada"));
        assertEquals(EstadoCita.CANCELADA, EstadoCita.desdeTexto("CANCELADA"));
    }
}
