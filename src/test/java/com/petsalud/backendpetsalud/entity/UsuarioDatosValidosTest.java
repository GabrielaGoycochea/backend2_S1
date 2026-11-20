package com.petsalud.backendpetsalud.entity;

import com.petsalud.backendpetsalud.dto.RegistroUsuarioRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S5 - Validacion de los datos del usuario (email, nombre y rol).
 * Email y nombre se validan con las restricciones del DTO de registro (el mismo que usa la API);
 * el rol se valida con el enum Rol (CLIENTE, VETERINARIO, ADMIN).
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

    @Test
    @DisplayName("Los roles validos del sistema son CLIENTE, VETERINARIO y ADMIN")
    void rol_valoresDelSistema_sonLosTresEsperados() {
        assertEquals(List.of(Rol.CLIENTE, Rol.VETERINARIO, Rol.ADMIN), List.of(Rol.values()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENTE", "VETERINARIO", "ADMIN"})
    @DisplayName("Los nombres CLIENTE, VETERINARIO y ADMIN se reconocen como roles validos")
    void rol_nombreValido_seReconoce(String nombre) {
        assertEquals(nombre, Rol.valueOf(nombre).name());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "EMPLEADO", "cliente", "SUPERADMIN", ""})
    @DisplayName("Un nombre que no es CLIENTE, VETERINARIO ni ADMIN no es un rol valido")
    void rol_nombreInvalido_lanzaError(String nombre) {
        assertThrows(IllegalArgumentException.class, () -> Rol.valueOf(nombre));
    }

    @Test
    @DisplayName("Un usuario construido con rol conserva ese rol")
    void usuario_conRol_conservaElRol() {
        Usuario usuario = Usuario.builder().email("juan.perez@example.com").nombre("Juan").rol(Rol.VETERINARIO).build();

        assertNotNull(usuario.getRol());
        assertEquals(Rol.VETERINARIO, usuario.getRol());
    }
}
