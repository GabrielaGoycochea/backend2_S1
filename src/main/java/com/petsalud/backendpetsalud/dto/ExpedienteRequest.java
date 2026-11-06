package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.ExpedienteMedico;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos clinicos de un registro del expediente. Son datos sensibles (salud): se valida el largo
 * y que no traigan HTML. La mascota y el veterinario llegan por parametros y el servicio
 * comprueba que el veterinario sea quien hace la solicitud.
 */
public record ExpedienteRequest(
        @NotBlank @Size(max = 500) @Pattern(regexp = Validaciones.TEXTO_SEGURO, message = Validaciones.MSG_TEXTO_SEGURO)
        String descripcion,

        @Size(max = 500) @Pattern(regexp = Validaciones.TEXTO_SEGURO, message = Validaciones.MSG_TEXTO_SEGURO)
        String diagnostico,

        @Size(max = 500) @Pattern(regexp = Validaciones.TEXTO_SEGURO, message = Validaciones.MSG_TEXTO_SEGURO)
        String tratamiento
) {
    public ExpedienteMedico aEntidad() {
        return ExpedienteMedico.builder()
                .descripcion(descripcion).diagnostico(diagnostico).tratamiento(tratamiento)
                .build();
    }
}
