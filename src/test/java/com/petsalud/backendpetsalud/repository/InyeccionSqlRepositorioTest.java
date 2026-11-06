package com.petsalud.backendpetsalud.repository;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Criterio 5 (SQL Injection en la capa de datos): los repositorios se ejecutan contra una base
 * H2 en memoria con SQL real de Hibernate. Las cargas tipicas de inyeccion se tratan como un
 * valor mas (parametro enlazado), por lo que no devuelven datos ni alteran las tablas.
 */
@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class InyeccionSqlRepositorioTest {

    @Autowired private UsuarioRepository usuarios;
    @Autowired private ProductoRepository productos;

    @BeforeEach
    void preparar() {
        usuarios.save(Usuario.builder().email("cliente@petsalud.cl").password("hash-bcrypt")
                .nombre("Cliente").rol(Rol.CLIENTE).build());
        usuarios.save(Usuario.builder().email("admin@petsalud.cl").password("hash-bcrypt")
                .nombre("Admin").rol(Rol.ADMIN).build());
        productos.save(Producto.builder().nombre("Collar").categoria("Accesorios")
                .precio(new BigDecimal("9990")).stock(10).build());
        productos.save(Producto.builder().nombre("Alimento").categoria("Alimentos")
                .precio(new BigDecimal("24990")).stock(20).build());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "' OR '1'='1",
            "' OR '1'='1' --",
            "cliente@petsalud.cl' --",
            "x' UNION SELECT * FROM usuarios --",
            "x'; DROP TABLE usuarios; --"
    })
    void buscarPorEmailNoSeAlteraConInyeccion(String carga) {
        assertTrue(usuarios.findByEmail(carga).isEmpty(), "la carga no debe devolver usuarios: " + carga);
        assertEquals(2, usuarios.count(), "la tabla usuarios sigue intacta");
        assertTrue(usuarios.findByEmail("cliente@petsalud.cl").isPresent());
    }

    @Test
    void buscarPorCategoriaNoSeAlteraConInyeccion() {
        assertTrue(productos.findByCategoria("Accesorios' OR '1'='1").isEmpty());
        assertEquals(1, productos.findByCategoria("Accesorios").size());
    }

    @Test
    void losRepositoriosNoUsanSqlNativo() throws Exception {
        List<Class<?>> repositorios = List.of(CitaRepository.class, ExpedienteMedicoRepository.class,
                MascotaRepository.class, NotificacionRepository.class, ProductoRepository.class,
                RecompensaRepository.class, UsuarioRepository.class, VentaRepository.class);
        for (Class<?> repo : repositorios) {
            for (Method metodo : repo.getDeclaredMethods()) {
                Query consulta = AnnotatedElementUtils.findMergedAnnotation(metodo, Query.class);
                assertTrue(consulta == null || !consulta.nativeQuery(),
                        repo.getSimpleName() + "." + metodo.getName() + " usa SQL nativo");
            }
        }
    }
}
