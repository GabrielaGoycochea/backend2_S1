package com.petsalud.backendpetsalud.entity;

import com.petsalud.backendpetsalud.dto.MascotaRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S5 - Relacion usuario-mascota.
 * Un CLIENTE puede tener una lista de mascotas y los datos de cada mascota (nombre y especie) deben ser validos.
 */
class UsuarioMascotaRelacionTest {

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

    private Usuario clienteConDosMascotas() {
        Usuario cliente = Usuario.builder().id(3L).email("ana@petsalud.cl").nombre("Ana")
                .rol(Rol.CLIENTE).mascotas(new ArrayList<>()).build();
        Mascota perro = Mascota.builder().id(1L).nombre("Firulais").especie("Perro").propietario(cliente).build();
        Mascota gato = Mascota.builder().id(2L).nombre("Michi").especie("Gato").propietario(cliente).build();
        cliente.getMascotas().add(perro);
        cliente.getMascotas().add(gato);
        return cliente;
    }

    @Test
    @DisplayName("Un usuario con rol CLIENTE puede tener una lista de mascotas asociadas")
    void cliente_puedeTenerListaDeMascotas() {
        Usuario cliente = clienteConDosMascotas();

        assertEquals(Rol.CLIENTE, cliente.getRol());
        assertEquals(2, cliente.getMascotas().size());
    }

    @Test
    @DisplayName("Cada mascota de la lista apunta a su propietario")
    void mascotas_apuntanAlPropietario() {
        Usuario cliente = clienteConDosMascotas();

        assertTrue(cliente.getMascotas().stream().allMatch(m -> m.getPropietario() == cliente));
    }

    @Test
    @DisplayName("Las mascotas del cliente tienen nombre y especie")
    void mascotasDelCliente_tienenNombreYEspecie() {
        List<Mascota> mascotas = clienteConDosMascotas().getMascotas();

        for (Mascota mascota : mascotas) {
            assertFalse(mascota.getNombre() == null || mascota.getNombre().isBlank());
            assertFalse(mascota.getEspecie() == null || mascota.getEspecie().isBlank());
        }
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
        Mascota mascota = new MascotaRequest("Firulais", "Perro", "Labrador", null, "Macho").aEntidad();

        assertEquals("Firulais", mascota.getNombre());
        assertEquals("Perro", mascota.getEspecie());
    }
}
