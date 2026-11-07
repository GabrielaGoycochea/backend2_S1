package com.petsalud.backendpetsalud.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos que un cliente puede enviar al registrarse.
 * NO incluye "rol" ni "id": asi nadie puede auto-asignarse ADMIN (mass assignment).
 */
public record RegistroUsuarioRequest(
        @NotBlank @Email @Size(max = 120)
        String email,

        // Politica de contrasenas: 8 a 72 caracteres (limite de BCrypt), al menos una letra y un numero
        @NotBlank
        @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "debe contener letras y numeros")
        String password,

        @NotBlank @Size(max = 60) @Pattern(regexp = "^[\\p{L} .'-]+$", message = "contiene caracteres no permitidos")
        String nombre,

        @Size(max = 60) @Pattern(regexp = "^[\\p{L} .'-]*$", message = "contiene caracteres no permitidos")
        String apellido,

        @Size(max = 20) @Pattern(regexp = "^[+0-9 ]*$", message = "solo numeros, espacios y +")
        String telefono,

        @Size(max = 150)
        String direccion
) {
}
