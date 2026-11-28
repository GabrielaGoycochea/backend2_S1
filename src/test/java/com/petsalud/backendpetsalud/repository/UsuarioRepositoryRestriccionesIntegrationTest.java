package com.petsalud.backendpetsalud.repository;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * S5 - Restricciones de la entidad Usuario aplicadas por la base de datos (JPA y H2, sin mocks).
 * Comprueba que el rol es obligatorio (@Column(nullable = false)) y que el email es unico
 * (@Column(unique = true)), aunque un dato invalido llegue a la capa de persistencia.
 */
@org.junit.jupiter.api.Tag("integration")
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class UsuarioRepositoryRestriccionesIntegrationTest {

    @Autowired private TestEntityManager em;

    @Test
    @DisplayName("La base de datos no permite guardar un usuario sin rol")
    void usuarioSinRol_noSePuedePersistir() {
        Usuario sinRol = Usuario.builder()
                .email("sinrol@prueba.cl")
                .password("hash")
                .nombre("Ana")
                .build();

        assertThrows(PersistenceException.class, () -> em.persistAndFlush(sinRol));
    }

    @Test
    @DisplayName("La base de datos no permite dos usuarios con el mismo email")
    void emailDuplicado_violaRestriccionUnica() {
        em.persistAndFlush(Usuario.builder()
                .email("repetido@prueba.cl")
                .password("hash")
                .nombre("Ana")
                .rol(Rol.CLIENTE)
                .build());
        Usuario duplicado = Usuario.builder()
                .email("repetido@prueba.cl")
                .password("hash")
                .nombre("Luis")
                .rol(Rol.CLIENTE)
                .build();

        assertThrows(PersistenceException.class, () -> em.persistAndFlush(duplicado));
    }
}
