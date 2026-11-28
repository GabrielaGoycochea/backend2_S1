package com.petsalud.backendpetsalud.repository;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.EstadoCita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Semana 6 (TDD, ciclo 3): la base de datos impide dos citas activas del mismo veterinario en el mismo
 * horario, aunque dos solicitudes pasen a la vez la verificacion previa del servicio.
 * Una cita cancelada no ocupa el horario. JPA y H2, sin mocks.
 */
@Tag("integration")
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class CitaRestriccionHorarioIntegrationTest {

    @Autowired private TestEntityManager em;
    @Autowired private CitaRepository citas;

    private final LocalDateTime fecha = LocalDateTime.of(2030, 1, 15, 10, 0);
    private Usuario veterinario;
    private Mascota mascota;

    @BeforeEach
    void preparar() {
        Usuario propietario = em.persistAndFlush(usuario("ana@prueba.cl", "Ana", Rol.CLIENTE));
        veterinario = em.persistAndFlush(usuario("luis@prueba.cl", "Luis", Rol.VETERINARIO));
        mascota = em.persistAndFlush(Mascota.builder()
                .nombre("Luna").especie("Gato").propietario(propietario).build());
    }

    private Usuario usuario(String email, String nombre, Rol rol) {
        return Usuario.builder().email(email).password("hash").nombre(nombre).rol(rol).build();
    }

    private Cita cita(Usuario vet, EstadoCita estado) {
        return Cita.builder().mascota(mascota).veterinario(vet).fechaHora(fecha)
                .estado(estado).motivo("Control").build();
    }

    @Test
    @DisplayName("Dos citas activas del mismo veterinario y horario: la base rechaza la segunda")
    void dosCitasActivas_mismoVeterinarioYHorario_violanLaRestriccionUnica() {
        citas.saveAndFlush(cita(veterinario, EstadoCita.AGENDADA));

        assertThrows(DataIntegrityViolationException.class,
                () -> citas.saveAndFlush(cita(veterinario, EstadoCita.AGENDADA)));
    }

    @Test
    @DisplayName("Una cita cancelada no ocupa el horario: se puede agendar otra en el mismo lugar")
    void citaCancelada_noOcupaElHorario() {
        citas.saveAndFlush(cita(veterinario, EstadoCita.CANCELADA));

        assertDoesNotThrow(() -> citas.saveAndFlush(cita(veterinario, EstadoCita.AGENDADA)));
        assertEquals(2, citas.count());
    }

    @Test
    @DisplayName("Al cancelar una cita activa, su horario queda libre")
    void cancelarUnaCitaActiva_liberaElHorario() {
        Cita primera = citas.saveAndFlush(cita(veterinario, EstadoCita.AGENDADA));
        primera.setEstado(EstadoCita.CANCELADA);
        citas.saveAndFlush(primera);

        assertDoesNotThrow(() -> citas.saveAndFlush(cita(veterinario, EstadoCita.AGENDADA)));
        assertEquals(2, citas.count());
    }

    @Test
    @DisplayName("Reactivar una cita cancelada cuyo horario ya se ocupo viola la restriccion")
    void reactivarCitaCancelada_enHorarioOcupado_violaLaRestriccion() {
        Cita cancelada = citas.saveAndFlush(cita(veterinario, EstadoCita.CANCELADA));
        citas.saveAndFlush(cita(veterinario, EstadoCita.AGENDADA));

        cancelada.setEstado(EstadoCita.AGENDADA);

        assertThrows(DataIntegrityViolationException.class, () -> citas.saveAndFlush(cancelada));
    }

    @Test
    @DisplayName("El mismo horario con otro veterinario no choca")
    void mismoHorario_otroVeterinario_noChoca() {
        Usuario otroVeterinario = em.persistAndFlush(usuario("pedro@prueba.cl", "Pedro", Rol.VETERINARIO));
        citas.saveAndFlush(cita(veterinario, EstadoCita.AGENDADA));

        assertDoesNotThrow(() -> citas.saveAndFlush(cita(otroVeterinario, EstadoCita.AGENDADA)));
    }
}
