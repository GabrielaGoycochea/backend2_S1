package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Campos de perfil que el gerente puede actualizar. No incluye email, contrasena ni rol,
 * asi que no se pueden cambiar por esta via (asignacion masiva).
 */
public record ActualizarUsuarioRequest(
        @NotBlank @Size(max = 60) @Pattern(regexp = Validaciones.NOMBRE_PERSONA, message = Validaciones.MSG_NOMBRE_PERSONA)
        String nombre,

        @Size(max = 60) @Pattern(regexp = Validaciones.NOMBRE_PERSONA, message = Validaciones.MSG_NOMBRE_PERSONA)
        String apellido,

        @Size(max = 20) @Pattern(regexp = Validaciones.TELEFONO, message = Validaciones.MSG_TELEFONO)
        String telefono,

        @Size(max = 150) @Pattern(regexp = Validaciones.TEXTO_SEGURO, message = Validaciones.MSG_TEXTO_SEGURO)
        String direccion
) {
    public Usuario aEntidad() {
        return Usuario.builder()
                .nombre(nombre).apellido(apellido).telefono(telefono).direccion(direccion)
                .build();
    }
}
