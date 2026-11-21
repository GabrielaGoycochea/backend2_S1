package com.petsalud.backendpetsalud.repository;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S5 - Prueba de integracion de la consulta de disponibilidad con JPA y H2 (sin mocks).
 * Comprueba la semantica real de existsByVeterinarioIdAndFechaHoraAndEstadoNot: que cita bloquea
 * el horario de un veterinario y cual no.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class CitaRepositoryDisponibilidadIntegrationTest {

    @Autowired private TestEntityManager em;
    @Autowired private CitaRepository citas;

    private final LocalDateTime fecha = LocalDateTime.of(2030, 1, 15, 10, 0);
    private Usuario veterinario;
    private Mascota mascota;

    @BeforeEach
    void preparar() {
        Usuario propietario = em.persistAndFlush(Usuario.builder()
                .email("propietario@prueba.cl")
                .password("hash")
                .nombre("Ana")
                .rol(Rol.CLIENTE)
                .build());
        veterinario = em.persistAndFlush(Usuario.builder()
                .email("veterinario@prueba.cl")
                .password("hash")
                .nombre("Luis")
                .rol(Rol.VETERINARIO)
                .build());
        mascota = em.persistAndFlush(Mascota.builder()
                .nombre("Luna")
                .especie("Gato")
                .propietario(propietario)
                .build());
    }

    private void guardarCita(String estado) {
        em.persistAndFlush(Cita.builder()
                .mascota(mascota)
                .veterinario(veterinario)
                .fechaHora(fecha)
                .estado(estado)
                .motivo("Control")
                .build());
    }

    @Test
    @DisplayName("Una cita Agendada ocupa el horario del veterinario")
    void citaAgendada_ocupaHorarioDelVeterinario() {
        guardarCita("Agendada");

        assertTrue(citas.existsByVeterinarioIdAndFechaHoraAndEstadoNot(veterinario.getId(), fecha, "Cancelada"));
    }

    @Test
    @DisplayName("Una cita Cancelada deja el horario disponible")
    void citaCancelada_dejaHorarioDisponible() {
        guardarCita("Cancelada");

        assertFalse(citas.existsByVeterinarioIdAndFechaHoraAndEstadoNot(veterinario.getId(), fecha, "Cancelada"));
    }

    @Test
    @DisplayName("La cita de un veterinario no bloquea el mismo horario de otro veterinario")
    void citaDeOtroVeterinario_noBloqueaHorario() {
        guardarCita("Agendada");
        Usuario otroVeterinario = em.persistAndFlush(Usuario.builder()
                .email("otrovet@prueba.cl")
                .password("hash")
                .nombre("Pedro")
                .rol(Rol.VETERINARIO)
                .build());

        assertFalse(citas.existsByVeterinarioIdAndFechaHoraAndEstadoNot(otroVeterinario.getId(), fecha, "Cancelada"));
        assertTrue(citas.existsByVeterinarioIdAndFechaHoraAndEstadoNot(veterinario.getId(), fecha, "Cancelada"));
    }

    @Test
    @DisplayName("Una cita del mismo veterinario a otra hora no ocupa el horario consultado (solo se detecta coincidencia exacta)")
    void mismoVeterinario_otroHorario_noBloquea() {
        guardarCita("Agendada");

        assertFalse(citas.existsByVeterinarioIdAndFechaHoraAndEstadoNot(
                veterinario.getId(), fecha.plusMinutes(30), "Cancelada"));
    }
}
