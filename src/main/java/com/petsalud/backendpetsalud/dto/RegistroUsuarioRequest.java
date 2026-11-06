package com.petsalud.backendpetsalud.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos que un cliente puede enviar al registrarse.
 * NO incluye "rol" ni "id": asi nadie puede auto-asignarse ADMIN (mass assignment).
 * Semana 3: nombre y apellido ya no aceptan apostrofes ni comillas, y la direccion no acepta < ni >.
 */
public record RegistroUsuarioRequest(
        @NotBlank @Email @Size(max = 120)
        String email,

        // Politica de contrasenas: 8 a 72 caracteres (limite de BCrypt), al menos una letra y un numero
        @NotBlank
        @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "debe contener letras y numeros")
        String password,

        @NotBlank @Size(max = 60) @Pattern(regexp = Validaciones.NOMBRE_PERSONA, message = Validaciones.MSG_NOMBRE_PERSONA)
        String nombre,

        @Size(max = 60) @Pattern(regexp = Validaciones.NOMBRE_PERSONA, message = Validaciones.MSG_NOMBRE_PERSONA)
        String apellido,

        @Size(max = 20) @Pattern(regexp = Validaciones.TELEFONO, message = Validaciones.MSG_TELEFONO)
        String telefono,

        @Size(max = 150) @Pattern(regexp = Validaciones.TEXTO_SEGURO, message = Validaciones.MSG_TEXTO_SEGURO)
        String direccion
) {
}
