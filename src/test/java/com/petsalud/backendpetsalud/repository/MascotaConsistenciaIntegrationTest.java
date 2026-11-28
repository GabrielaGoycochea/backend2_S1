package com.petsalud.backendpetsalud.repository;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.EstadoCita;
import com.petsalud.backendpetsalud.entity.ExpedienteMedico;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Semana 6: consistencia de una mascota con sus citas y su expediente medico (JPA y H2, sin mocks).
 * Las citas y los registros de una mascota no se mezclan con los de otra, y siempre llevan al
 * mismo propietario.
 */
@Tag("integration")
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class MascotaConsistenciaIntegrationTest {

    @Autowired private TestEntityManager em;
    @Autowired private CitaRepository citas;
    @Autowired private ExpedienteMedicoRepository expedientes;

    private Usuario propietario;
    private Mascota luna;
    private Mascota rocky;

    @BeforeEach
    void preparar() {
        propietario = em.persistAndFlush(usuario("ana@prueba.cl", "Ana", Rol.CLIENTE));
        Usuario veterinario = em.persistAndFlush(usuario("luis@prueba.cl", "Luis", Rol.VETERINARIO));
        luna = em.persistAndFlush(mascota("Luna", "Gato"));
        rocky = em.persistAndFlush(mascota("Rocky", "Perro"));

        LocalDateTime fecha = LocalDateTime.of(2030, 1, 15, 10, 0);
        em.persist(Cita.builder().mascota(luna).veterinario(veterinario).fechaHora(fecha)
                .estado(EstadoCita.AGENDADA).motivo("Control").build());
        em.persist(Cita.builder().mascota(rocky).veterinario(veterinario).fechaHora(fecha.plusHours(1))
                .estado(EstadoCita.AGENDADA).motivo("Vacuna").build());
        em.persist(ExpedienteMedico.builder().mascota(luna).veterinario(veterinario).fecha(fecha)
                .descripcion("Control").diagnostico("Sana").tratamiento("Ninguno").build());
        em.persist(ExpedienteMedico.builder().mascota(rocky).veterinario(veterinario).fecha(fecha)
                .descripcion("Vacuna").diagnostico("Sano").tratamiento("Antirrabica").build());
        em.flush();
        em.clear(); // obliga a leer desde la base y no desde la memoria de Hibernate
    }

    private Usuario usuario(String email, String nombre, Rol rol) {
        return Usuario.builder().email(email).password("hash").nombre(nombre).rol(rol).build();
    }

    private Mascota mascota(String nombre, String especie) {
        return Mascota.builder().nombre(nombre).especie(especie).propietario(propietario).build();
    }

    @Test
    @DisplayName("Las citas de una mascota son solo las suyas y llevan a su propietario")
    void citasDeUnaMascota_sonSoloLasSuyas() {
        List<Cita> citasDeLuna = citas.findByMascotaId(luna.getId());

        assertEquals(1, citasDeLuna.size());
        assertEquals("Control", citasDeLuna.get(0).getMotivo());
        assertEquals(propietario.getId(), citasDeLuna.get(0).getMascota().getPropietario().getId());
    }

    @Test
    @DisplayName("El expediente de una mascota contiene solo sus registros, con los datos clinicos completos")
    void expedienteDeUnaMascota_contieneSoloSusRegistros() {
        List<ExpedienteMedico> registros = expedientes.findByMascotaId(rocky.getId());

        assertEquals(1, registros.size());
        assertEquals("Vacuna", registros.get(0).getDescripcion());
        assertEquals("Antirrabica", registros.get(0).getTratamiento());
        assertEquals(propietario.getId(), registros.get(0).getMascota().getPropietario().getId());
    }
}
