package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Notificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Notificacion que envia el gerente (promociones, campanas de adopcion, recordatorios). */
public record NotificacionRequest(
        @NotBlank @Size(max = 500) @Pattern(regexp = Validaciones.TEXTO_SEGURO, message = Validaciones.MSG_TEXTO_SEGURO)
        String mensaje,

        @NotBlank @Size(max = 30) @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE)
        String tipo
) {
    public Notificacion aEntidad() {
        return Notificacion.builder().mensaje(mensaje).tipo(tipo).build();
    }
}
