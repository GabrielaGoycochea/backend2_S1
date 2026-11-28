package com.petsalud.backendpetsalud.entity;

import java.util.Locale;

/**
 * Semana 6: estados posibles de una cita. Reemplaza al texto libre, donde "Cancelada" y "cancelada"
 * eran estados distintos para la consulta de disponibilidad.
 */
public enum EstadoCita {
    AGENDADA, CANCELADA, COMPLETADA;

    /** Convierte el texto de la API ("Agendada", "cancelada", " COMPLETADA ") en un estado valido. */
    public static EstadoCita desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El estado de la cita es obligatorio");
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Estado de cita no valido: " + texto + " (use Agendada, Cancelada o Completada)");
        }
    }
}
