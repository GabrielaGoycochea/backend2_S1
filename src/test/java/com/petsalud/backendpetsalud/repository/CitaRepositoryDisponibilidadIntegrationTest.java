package com.petsalud.backendpetsalud.repository;

import com.petsalud.backendpetsalud.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "spring.datasource.username=sa", "spring.datasource.password="})
class CitaRepositoryDisponibilidadIntegrationTest {
    @Autowired private TestEntityManager em;
    @Autowired private CitaRepository citas;
    private Usuario veterinario;
    private Mascota mascota;
    private final LocalDateTime fecha = LocalDateTime.of(2027, 1, 15, 10, 0);

    @BeforeEach
    void preparar() {
        Usuario propietario = em.persistAndFlush(Usuario.builder().email("propietario@prueba.cl").password("hash").nombre("Ana").rol(Rol.CLIENTE).build());
        veterinario = em.persistAndFlush(Usuario.builder().email("veterinario@prueba.cl").password("hash").nombre("Luis").rol(Rol.VETERINARIO).build());
        mascota = em.persistAndFlush(Mascota.builder().nombre("Luna").especie("Gato").propietario(propietario).build());
    }

    private void guardarCita(String estado) {
        em.persistAndFlush(Cita.builder().mascota(mascota).veterinario(veterinario).fechaHora(fecha).estado(estado).motivo("Control").build());
    }

    @Test
    void citaAgendada_ocupaHorarioDelVeterinario() {
        guardarCita("Agendada");
        assertTrue(citas.existsByVeterinarioIdAndFechaHoraAndEstadoNot(veterinario.getId(), fecha, "Cancelada"));
    }

    @Test
    void citaCancelada_dejaHorarioDisponible() {
        guardarCita("Cancelada");
        assertFalse(citas.existsByVeterinarioIdAndFechaHoraAndEstadoNot(veterinario.getId(), fecha, "Cancelada"));
    }

    @Test
    void citaDeOtroVeterinario_noBloqueaHorario() {
        guardarCita("Agendada");
        Usuario otro = em.persistAndFlush(Usuario.builder().email("otrovet@prueba.cl").password("hash").nombre("Pedro").rol(Rol.VETERINARIO).build());
        assertFalse(citas.existsByVeterinarioIdAndFechaHoraAndEstadoNot(otro.getId(), fecha, "Cancelada"));
        assertTrue(citas.existsByVeterinarioIdAndFechaHoraAndEstadoNot(veterinario.getId(), fecha, "Cancelada"));
    }
}
