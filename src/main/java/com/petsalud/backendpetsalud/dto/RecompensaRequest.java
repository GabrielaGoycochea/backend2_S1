package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Recompensa;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Puntos de fidelizacion que asigna el gerente. */
public record RecompensaRequest(
        @NotNull @Positive @Max(100_000)
        Integer puntos,

        @Size(max = 255) @Pattern(regexp = Validaciones.TEXTO_SEGURO, message = Validaciones.MSG_TEXTO_SEGURO)
        String descripcion
) {
    public Recompensa aEntidad() {
        return Recompensa.builder().puntos(puntos).descripcion(descripcion).build();
    }
}
