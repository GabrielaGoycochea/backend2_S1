package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.EstadoCita;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.CitaRepository;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import org.springframework.test.context.transaction.TestTransaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S5 - Agendamiento de citas de punta a punta: servicio real, repositorios reales y H2.
 * Solo se simula AutorizacionService, porque depende del usuario autenticado y tiene sus propias pruebas.
 * Une la regla de horario ocupado de CitaServiceImpl con la consulta real de CitaRepository.
 */
@org.junit.jupiter.api.Tag("integration")
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@Import(CitaServiceImpl.class)
class CitaAgendamientoIntegrationTest {

    @Autowired private CitaService citaService;
    @Autowired private CitaRepository citaRepository;
    @Autowired private TestEntityManager em;
    @MockBean private AutorizacionService autorizacionService;

    private final LocalDateTime fecha = LocalDateTime.of(2030, 1, 15, 10, 0);
    private Long mascotaId;
    private Long veterinarioId;

    @BeforeEach
    void prepararDatos() {
        Usuario propietario = em.persist(Usuario.builder()
                .email("ana@prueba.cl")
                .password("hash")
                .nombre("Ana")
                .rol(Rol.CLIENTE)
                .build());
        Usuario veterinario = em.persist(Usuario.builder()
                .email("luis@prueba.cl")
                .password("hash")
                .nombre("Luis")
                .rol(Rol.VETERINARIO)
                .build());
        Mascota mascota = em.persist(Mascota.builder()
                .nombre("Luna")
                .especie("Gato")
                .propietario(propietario)
                .build());
        em.flush();
        mascotaId = mascota.getId();
        veterinarioId = veterinario.getId();
    }

    @Test
    @DisplayName("Una segunda cita con el mismo veterinario, fecha y hora se rechaza y solo queda la primera")
    void segundaCitaEnHorarioOcupado_seRechazaYSoloQuedaUna() {
        citaService.agendarCita(mascotaId, veterinarioId, fecha, "Control");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.agendarCita(mascotaId, veterinarioId, fecha, "Vacuna"));
        assertTrue(error.getMessage().contains("horario"));
        assertEquals(1, citaRepository.count());
    }

    @Test
    @DisplayName("Si la cita del horario esta Cancelada, el horario puede volver a agendarse")
    void horarioConCitaCancelada_puedeVolverAAgendarse() {
        Cita primera = citaService.agendarCita(mascotaId, veterinarioId, fecha, "Control");
        citaService.cancelarCita(primera.getId());
        em.flush();

        Cita nueva = citaService.agendarCita(mascotaId, veterinarioId, fecha, "Vacuna");

        assertEquals(EstadoCita.AGENDADA, nueva.getEstado());
        assertEquals(2, citaRepository.count());
        em.flush();
        em.clear();
        assertEquals(EstadoCita.CANCELADA, citaRepository.findById(primera.getId()).orElseThrow().getEstado());
    }

    @Test
    @DisplayName("Dos solicitudes simultaneas permiten una sola reserva")
    void solicitudesSimultaneas_soloUnaReserva() throws Exception {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrera = new CyclicBarrier(2);
        Callable<Boolean> solicitud = () -> {
            barrera.await(10, TimeUnit.SECONDS);
            try {
                citaService.agendarCita(mascotaId, veterinarioId, fecha, "Control");
                return true;
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains("horario"));
                return false;
            }
        };
        try {
            Future<Boolean> primera = ejecutor.submit(solicitud);
            Future<Boolean> segunda = ejecutor.submit(solicitud);
            assertEquals(1, (primera.get(20, TimeUnit.SECONDS) ? 1 : 0) + (segunda.get(20, TimeUnit.SECONDS) ? 1 : 0));
            assertEquals(1, citaRepository.findByMascotaId(mascotaId).size());
        } finally {
            ejecutor.shutdownNow();
            ejecutor.awaitTermination(10, TimeUnit.SECONDS);
            TestTransaction.start();
            Mascota mascota = em.find(Mascota.class, mascotaId);
            Usuario propietario = mascota.getPropietario();
            em.getEntityManager().createQuery("delete from Cita c where c.mascota.id = :id").setParameter("id", mascotaId).executeUpdate();
            em.remove(mascota);
            em.flush();
            em.remove(propietario);
            em.remove(em.find(Usuario.class, veterinarioId));
            TestTransaction.flagForCommit();
            TestTransaction.end();
        }
    }

    @Test
    @DisplayName("No se reactiva una cita cancelada si su horario fue reservado")
    void reactivarCitaCancelada_horarioOcupado_seRechaza() {
        Cita primera = citaService.agendarCita(mascotaId, veterinarioId, fecha, "Control");
        citaService.cancelarCita(primera.getId());
        citaService.agendarCita(mascotaId, veterinarioId, fecha, "Vacuna");
        org.mockito.Mockito.when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(Usuario.builder().rol(Rol.ADMIN).build());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> citaService.actualizarEstadoCita(primera.getId(), "Agendada"));
        assertTrue(error.getMessage().contains("horario"));
        assertEquals(EstadoCita.CANCELADA, primera.getEstado());
    }
}
