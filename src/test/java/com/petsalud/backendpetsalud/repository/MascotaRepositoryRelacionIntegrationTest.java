package com.petsalud.backendpetsalud.repository;

import org.junit.jupiter.api.Tag;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * S5 - Relacion usuario-mascota con persistencia real (JPA y H2, sin mocks).
 * Comprueba la clave foranea usuario_id, la consulta por propietario y la lista de mascotas
 * del usuario (mappedBy = "propietario").
 * Las comparaciones se hacen por nombre y especie, no por entidades completas, porque Usuario y
 * Mascota usan @Data de Lombok con relaciones bidireccionales.
 */
@Tag("integration")
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class MascotaRepositoryRelacionIntegrationTest {

    @Autowired private TestEntityManager em;
    @Autowired private MascotaRepository mascotaRepository;

    private Long clienteId;

    @BeforeEach
    void prepararDatos() {
        Usuario cliente = em.persist(usuario("ana@prueba.cl", "Ana"));
        Usuario otroCliente = em.persist(usuario("pedro@prueba.cl", "Pedro"));
        em.persist(mascota("Firulais", "Perro", cliente));
        em.persist(mascota("Michi", "Gato", cliente));
        em.persist(mascota("Rocky", "Perro", otroCliente));
        em.flush();
        clienteId = cliente.getId();
        em.clear(); // obliga a leer desde la base y no desde la memoria de Hibernate
    }

    @Test
    @DisplayName("La consulta por propietario devuelve solo las mascotas de ese CLIENTE, con nombre y especie")
    void findByPropietarioId_devuelveSoloLasMascotasDelPropietario() {
        List<Mascota> resultado = mascotaRepository.findByPropietarioId(clienteId);

        assertEquals(2, resultado.size());
        assertEquals(Set.of("Firulais:Perro", "Michi:Gato"),
                resultado.stream()
                        .map(m -> m.getNombre() + ":" + m.getEspecie())
                        .collect(Collectors.toSet()));
        resultado.forEach(m -> assertEquals(clienteId, m.getPropietario().getId()));
    }

    @Test
    @DisplayName("Un CLIENTE cargado desde la base tiene su lista de mascotas asociadas")
    void cliente_cargadoDesdeLaBase_tieneSuListaDeMascotas() {
        Usuario cliente = em.find(Usuario.class, clienteId);

        assertEquals(Rol.CLIENTE, cliente.getRol());
        assertEquals(2, cliente.getMascotas().size());
        assertEquals(Set.of("Firulais", "Michi"),
                cliente.getMascotas().stream()
                        .map(Mascota::getNombre)
                        .collect(Collectors.toSet()));
    }

    private Usuario usuario(String email, String nombre) {
        return Usuario.builder()
                .email(email)
                .password("hash")
                .nombre(nombre)
                .rol(Rol.CLIENTE)
                .build();
    }

    private Mascota mascota(String nombre, String especie, Usuario propietario) {
        return Mascota.builder()
                .nombre(nombre)
                .especie(especie)
                .propietario(propietario)
                .build();
    }
}
