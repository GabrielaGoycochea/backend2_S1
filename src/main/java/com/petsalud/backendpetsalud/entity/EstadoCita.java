package com.petsalud.backendpetsalud.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum EstadoCita {
    AGENDADA("Agendada"),
    CANCELADA("Cancelada"),
    COMPLETADA("Completada");

    private final String texto;

    EstadoCita(String texto) {
        this.texto = texto;
    }

    @JsonValue
    public String getTexto() {
        return texto;
    }

    @JsonCreator
    public static EstadoCita desdeTexto(String texto) {
        for (EstadoCita estado : values()) {
            if (estado.texto.equalsIgnoreCase(texto)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de cita invalido: " + texto);
    }

    @Converter
    public static class Conversor implements AttributeConverter<EstadoCita, String> {
        @Override
        public String convertToDatabaseColumn(EstadoCita estado) {
            return estado == null ? null : estado.getTexto();
        }

        @Override
        public EstadoCita convertToEntityAttribute(String texto) {
            return texto == null ? null : EstadoCita.desdeTexto(texto);
        }
    }
}
