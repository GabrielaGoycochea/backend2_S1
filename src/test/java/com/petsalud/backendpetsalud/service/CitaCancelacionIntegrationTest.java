package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.EstadoCita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.CitaRepository;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Semana 6 (TDD, ciclo 2): cancelar una cita cambia su estado y conserva el registro.
 * Servicio real, repositorio real y H2; solo se simula AutorizacionService.
 */
@Tag("integration")
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@Import(CitaServiceImpl.class)
class CitaCancelacionIntegrationTest {

    @Autowired private CitaService citaService;
    @Autowired private CitaRepository citaRepository;
    @Autowired private TestEntityManager em;
    @MockBean private AutorizacionService autorizacionService;

    private final LocalDateTime fecha = LocalDateTime.of(2030, 1, 15, 10, 0);
    private Long mascotaId;
    private Long veterinarioId;
    private Usuario admin;

    @BeforeEach
    void prepararDatos() {
        Usuario propietario = em.persist(usuario("ana@prueba.cl", "Ana", Rol.CLIENTE));
        Usuario veterinario = em.persist(usuario("luis@prueba.cl", "Luis", Rol.VETERINARIO));
        admin = em.persist(usuario("admin@prueba.cl", "Admin", Rol.ADMIN));
        Mascota mascota = em.persist(Mascota.builder()
                .nombre("Luna").especie("Gato").propietario(propietario).build());
        em.flush();
        mascotaId = mascota.getId();
        veterinarioId = veterinario.getId();
    }

    private Usuario usuario(String email, String nombre, Rol rol) {
        return Usuario.builder().email(email).password("hash").nombre(nombre).rol(rol).build();
    }

    @Test
    @DisplayName("Cancelar una cita no la borra: el registro queda con estado CANCELADA")
    void cancelarCita_conservaElRegistroConEstadoCancelada() {
        Long id = citaService.agendarCita(mascotaId, veterinarioId, fecha, "Control").getId();

        citaService.cancelarCita(id);
        em.flush();
        em.clear(); // obliga a leer desde la base y no desde la memoria de Hibernate

        Cita guardada = citaRepository.findById(id).orElseThrow();
        assertEquals(EstadoCita.CANCELADA, guardada.getEstado());
        assertEquals(1, citaRepository.count());
    }

    @Test
    @DisplayName("Cancelo una cita y luego puedo agendar otra en el mismo horario")
    void cancelarYReagendar_enElMismoHorario_funciona() {
        Long primeraId = citaService.agendarCita(mascotaId, veterinarioId, fecha, "Control").getId();
        citaService.cancelarCita(primeraId);

        Cita segunda = citaService.agendarCita(mascotaId, veterinarioId, fecha, "Vacuna");

        assertEquals(EstadoCita.AGENDADA, segunda.getEstado());
        assertEquals(EstadoCita.CANCELADA, citaRepository.findById(primeraId).orElseThrow().getEstado());
        assertEquals(2, citaRepository.count());
    }

    @Test
    @DisplayName("No se puede reactivar la cita cancelada si otra ya ocupo el horario")
    void reactivarCitaCancelada_siElHorarioYaSeOcupo_seRechaza() {
        Long primeraId = citaService.agendarCita(mascotaId, veterinarioId, fecha, "Control").getId();
        citaService.cancelarCita(primeraId);
        citaService.agendarCita(mascotaId, veterinarioId, fecha, "Vacuna");
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(admin);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.actualizarEstadoCita(primeraId, "Agendada"));

        assertTrue(error.getMessage().contains("horario"));
    }
}
