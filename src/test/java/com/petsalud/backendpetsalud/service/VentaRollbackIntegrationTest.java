package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.*;
import com.petsalud.backendpetsalud.repository.*;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:petsalud_rollback;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class VentaRollbackIntegrationTest {
    @Autowired private VentaService ventaService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProductoRepository productoRepository;
    @MockBean private VentaRepository ventaRepository;
    @MockBean private AutorizacionService autorizacionService;

    @Test
    void errorAlGuardarVenta_revierteDescuentosEnBaseDeDatos() {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder().email("rollback@test.cl").password("test").nombre("Test").apellido("Rollback").direccion("Calle 1").rol(Rol.CLIENTE).build());
        Producto primero = productoRepository.saveAndFlush(Producto.builder().nombre("Collar rollback").precio(new BigDecimal("5990")).stock(5).build());
        Producto segundo = productoRepository.saveAndFlush(Producto.builder().nombre("Alimento rollback").precio(new BigDecimal("18990")).stock(3).build());

        when(ventaRepository.save(any(Venta.class))).thenThrow(new IllegalStateException("Fallo de persistencia simulado"));

        assertThrows(IllegalStateException.class, () -> ventaService.realizarVenta(usuario.getId(), List.of(primero.getId(), segundo.getId())));

        assertEquals(5, productoRepository.findById(primero.getId()).orElseThrow().getStock());
        assertEquals(3, productoRepository.findById(segundo.getId()).orElseThrow().getStock());
        verify(ventaRepository).save(any(Venta.class));
    }
}
