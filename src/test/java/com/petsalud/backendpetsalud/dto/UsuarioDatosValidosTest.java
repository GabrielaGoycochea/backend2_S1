package com.petsalud.backendpetsalud.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S5 - Validacion de los datos del usuario: email y nombre obligatorios y con formato valido.
 * Se validan las restricciones de RegistroUsuarioRequest, el DTO que usa la API para registrar usuarios.
 * La validez del rol se prueba donde el sistema lo usa: UsuarioServiceImplTest (el registro siempre
 * asigna CLIENTE), CitaServiceImplTest (solo un VETERINARIO atiende citas) y
 * UsuarioRepositoryRestriccionesIntegrationTest (la base no acepta usuarios sin rol).
 */
class UsuarioDatosValidosTest {

    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private RegistroUsuarioRequest registro(String email, String nombre) {
        return new RegistroUsuarioRequest(email, "Clave1234", nombre, "Perez", "+56912345678", "Calle Falsa 123");
    }

    private Set<String> camposInvalidos(RegistroUsuarioRequest solicitud) {
        return validator.validate(solicitud).stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Un usuario con email y nombre validos no tiene errores de validacion")
    void datosCompletos_noTieneErrores() {
        assertTrue(camposInvalidos(registro("juan.perez@example.com", "Juan")).isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("El email no puede ser nulo, vacio ni estar en blanco")
    void email_nuloVacioOEnBlanco_esInvalido(String email) {
        assertTrue(camposInvalidos(registro(email, "Juan")).contains("email"));
    }

    @Test
    @DisplayName("El email debe tener formato valido")
    void email_formatoInvalido_esInvalido() {
        assertTrue(camposInvalidos(registro("esto-no-es-un-correo", "Juan")).contains("email"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("El nombre no puede ser nulo, vacio ni estar en blanco")
    void nombre_nuloVacioOEnBlanco_esInvalido(String nombre) {
        assertTrue(camposInvalidos(registro("juan.perez@example.com", nombre)).contains("nombre"));
    }

    @Test
    @DisplayName("El nombre no admite caracteres de HTML")
    void nombre_conCaracteresPeligrosos_esInvalido() {
        assertTrue(camposInvalidos(registro("juan.perez@example.com", "Juan<script>")).contains("nombre"));
    }
}
