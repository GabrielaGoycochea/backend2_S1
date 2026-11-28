package com.petsalud.backendpetsalud.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Semana 5: regresión de las relaciones bidireccionales.
 * Con @Data de Lombok, Usuario.toString() llamaba a mascotas.toString(), que volvía a llamar a
 * propietario.toString() y terminaba en StackOverflowError. El JSON ya estaba protegido con @JsonIgnore,
 * pero toString, equals y hashCode no. Estas pruebas fallan sin la corrección y pasan con ella.
 */
@DisplayName("Relaciones bidireccionales sin recursión infinita")
class RelacionesBidireccionalesTest {

    private Usuario cliente;
    private Mascota luna;

    @BeforeEach
    void enlazarEntidades() {
        // Arrange común: un cliente y su mascota, enlazados en ambos sentidos como lo hace JPA
        cliente = Usuario.builder()
                .id(1L).email("ana.rojas@petsalud.cl").password("Clave#2026").nombre("Ana").rol(Rol.CLIENTE)
                .mascotas(new ArrayList<>()).ventas(new ArrayList<>())
                .build();
        luna = Mascota.builder()
                .id(10L).nombre("Luna").especie("Perro").propietario(cliente).citas(new ArrayList<>())
                .build();
        cliente.getMascotas().add(luna);
    }

    @Test
    @DisplayName("Usuario con mascotas: toString, hashCode y equals no entran en recursión")
    void usuarioConMascotas_noProvocaStackOverflow() {
        // Act + Assert: cada llamada debe terminar sin StackOverflowError
        assertAll(
                () -> assertDoesNotThrow(cliente::toString),
                () -> assertDoesNotThrow(cliente::hashCode),
                () -> assertDoesNotThrow(() -> cliente.equals(luna.getPropietario())),
                () -> assertDoesNotThrow(luna::toString));
        assertFalse(cliente.toString().contains("Clave#2026"), "toString no debe exponer la contraseña");
    }

    @Test
    @DisplayName("Mascota con citas y usuario con ventas: tampoco entran en recursión")
    void mascotaConCitasYUsuarioConVentas_noProvocaStackOverflow() {
        // Arrange
        Usuario veterinario = Usuario.builder().id(2L).email("vet@petsalud.cl").nombre("Carlos").rol(Rol.VETERINARIO).build();
        Cita control = Cita.builder().id(50L).fechaHora(LocalDateTime.of(2026, 12, 1, 10, 0)).motivo("Control")
                .estado(EstadoCita.AGENDADA).mascota(luna).veterinario(veterinario).build();
        luna.getCitas().add(control);
        Venta venta = Venta.builder().id(7L).fecha(LocalDateTime.of(2026, 11, 20, 12, 0))
                .total(new BigDecimal("24980")).usuario(cliente).productos(new ArrayList<>()).build();
        cliente.getVentas().add(venta);

        // Act + Assert
        assertAll(
                () -> assertDoesNotThrow(control::toString),
                () -> assertDoesNotThrow(control::hashCode),
                () -> assertDoesNotThrow(venta::toString),
                () -> assertDoesNotThrow(venta::hashCode));
    }

    @Test
    @DisplayName("equals compara los datos de la entidad, no sus listas relacionadas")
    void equals_ignoraListasRelacionadas() {
        // Arrange: el mismo cliente, pero sin mascotas cargadas
        Usuario mismoClienteSinMascotas = Usuario.builder()
                .id(1L).email("ana.rojas@petsalud.cl").password("Clave#2026").nombre("Ana").rol(Rol.CLIENTE)
                .mascotas(new ArrayList<>()).ventas(new ArrayList<>())
                .build();

        // Act
        boolean iguales = cliente.equals(mismoClienteSinMascotas);

        // Assert
        assertTrue(iguales);
        assertEquals(cliente.hashCode(), mismoClienteSinMascotas.hashCode());
    }
}
