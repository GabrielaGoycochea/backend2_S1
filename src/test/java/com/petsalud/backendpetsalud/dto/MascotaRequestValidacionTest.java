package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Mascota;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S5 - Validacion de los datos de una mascota (MascotaRequest): nombre y especie obligatorios,
 * caracteres permitidos y fecha de nacimiento no futura.
 * La relacion usuario-mascota se prueba con persistencia real en MascotaRepositoryRelacionIntegrationTest.
 */
class MascotaRequestValidacionTest {

    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Set<String> camposInvalidos(MascotaRequest solicitud) {
        return validator.validate(solicitud).stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Una mascota con nombre y especie validos no tiene errores de validacion")
    void mascotaValida_noTieneErrores() {
        MascotaRequest solicitud = new MascotaRequest("Firulais", "Perro", "Labrador", LocalDate.of(2020, 5, 1), "Macho");

        assertTrue(camposInvalidos(solicitud).isEmpty());
    }

    @Test
    @DisplayName("El nombre de la mascota no puede estar vacio")
    void mascota_nombreVacio_esInvalido() {
        MascotaRequest solicitud = new MascotaRequest("", "Perro", null, null, null);

        assertTrue(camposInvalidos(solicitud).contains("nombre"));
    }

    @Test
    @DisplayName("La especie de la mascota no puede ser nula")
    void mascota_especieNula_esInvalida() {
        MascotaRequest solicitud = new MascotaRequest("Firulais", null, null, null, null);

        assertTrue(camposInvalidos(solicitud).contains("especie"));
    }

    @Test
    @DisplayName("El nombre de la mascota no admite caracteres peligrosos")
    void mascota_nombreConCaracteresPeligrosos_esInvalido() {
        MascotaRequest solicitud = new MascotaRequest("Firulais'; DROP TABLE", "Perro", null, null, null);

        assertTrue(camposInvalidos(solicitud).contains("nombre"));
    }

    @Test
    @DisplayName("La fecha de nacimiento de la mascota no puede estar en el futuro")
    void mascota_fechaNacimientoFutura_esInvalida() {
        MascotaRequest solicitud = new MascotaRequest("Firulais", "Perro", null, LocalDate.now().plusDays(10), null);

        assertTrue(camposInvalidos(solicitud).contains("fechaNacimiento"));
    }

    @Test
    @DisplayName("La solicitud valida se convierte en una mascota con los mismos datos")
    void mascotaRequest_aEntidad_conservaLosDatos() {
        Mascota mascota = new MascotaRequest("Firulais", "Perro", "Labrador", LocalDate.of(2020, 5, 1), "Macho").aEntidad();

        assertEquals("Firulais", mascota.getNombre());
        assertEquals("Perro", mascota.getEspecie());
        assertEquals("Labrador", mascota.getRaza());
        assertEquals(LocalDate.of(2020, 5, 1), mascota.getFechaNacimiento());
        assertEquals("Macho", mascota.getSexo());
    }
}
