package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Mascota;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos que se aceptan al registrar o actualizar una mascota.
 * No incluye id ni propietario: el propietario sale de la URL y el servicio comprueba que
 * corresponda al usuario autenticado (evita asignacion masiva y sobrescribir mascotas ajenas).
 */
public record MascotaRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE)
        String nombre,

        @NotBlank @Size(max = 30) @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE)
        String especie,

        @Size(max = 50) @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE)
        String raza,

        @PastOrPresent
        LocalDate fechaNacimiento,

        @Size(max = 10) @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE)
        String sexo
) {
    public Mascota aEntidad() {
        return Mascota.builder()
                .nombre(nombre).especie(especie).raza(raza)
                .fechaNacimiento(fechaNacimiento).sexo(sexo)
                .build();
    }
}
