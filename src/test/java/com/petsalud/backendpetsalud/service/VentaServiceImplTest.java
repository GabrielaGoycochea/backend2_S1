package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.entity.Venta;
import com.petsalud.backendpetsalud.repository.ProductoRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.repository.VentaRepository;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Semana 4: pruebas unitarias de las ventas.
 * Los repositorios y la autorizacion se simulan con Mockito: no se usa MySQL.
 */
@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private AutorizacionService autorizacionService;

    @InjectMocks
    private VentaServiceImpl ventaService;

    private Usuario usuario;
    private Producto collar;
    private Producto alimento;

    @BeforeEach
    void prepararDatos() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Juan")
                .apellido("Perez")
                .email("juan.perez@example.com")
                .direccion("Calle Falsa 123")
                .rol(Rol.CLIENTE)
                .build();

        collar = Producto.builder()
                .id(10L).nombre("Collar").precio(new BigDecimal("5990")).stock(5).build();

        alimento = Producto.builder()
                .id(20L).nombre("Alimento").precio(new BigDecimal("18990")).stock(3).build();
    }

    // La base de datos simulada devuelve al usuario de la prueba
    private void simularUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
    }

    // La base de datos simulada devuelve los productos y "guarda" la venta
    private void simularProductosYGuardado() {
        when(productoRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(collar, alimento));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(llamada -> llamada.getArgument(0));
    }

    // ---------- Datos obligatorios del usuario (criterio 2) ----------

    @Test
    @DisplayName("No registra la venta si el usuario no tiene nombre")
    void realizarVenta_usuarioSinNombre_lanzaError() {
        usuario.setNombre(null);
        simularUsuario();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L, 20L)));

        assertEquals("El usuario no tiene nombre registrado", error.getMessage());
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("No registra la venta si el usuario no tiene apellido")
    void realizarVenta_usuarioSinApellido_lanzaError() {
        usuario.setApellido("");
        simularUsuario();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L, 20L)));

        assertEquals("El usuario no tiene apellido registrado", error.getMessage());
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("No registra la venta si el usuario no tiene email")
    void realizarVenta_usuarioSinEmail_lanzaError() {
        usuario.setEmail("   ");
        simularUsuario();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L, 20L)));

        assertEquals("El usuario no tiene email registrado", error.getMessage());
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("No registra la venta si el usuario no tiene direccion")
    void realizarVenta_usuarioSinDireccion_lanzaError() {
        usuario.setDireccion(null);
        simularUsuario();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L, 20L)));

        assertEquals("El usuario no tiene direccion registrada", error.getMessage());
        verify(ventaRepository, never()).save(any());
    }

    // ---------- Comportamientos segun condiciones (criterio 3) ----------

    @Test
    @DisplayName("Un usuario con rol ADMIN puede realizar ventas")
    void realizarVenta_rolAdmin_registraLaVenta() {
        usuario.setRol(Rol.ADMIN);
        simularUsuario();
        simularProductosYGuardado();

        Venta venta = ventaService.realizarVenta(1L, List.of(10L, 20L));

        assertNotNull(venta);
        verify(ventaRepository).save(any(Venta.class));
    }

    @Test
    @DisplayName("Un usuario con rol VETERINARIO no puede realizar ventas")
    void realizarVenta_rolVeterinario_lanzaAccesoDenegado() {
        usuario.setRol(Rol.VETERINARIO);
        simularUsuario();

        assertThrows(AccessDeniedException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L, 20L)));

        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("No registra la venta si un producto no tiene stock")
    void realizarVenta_productoSinStock_lanzaError() {
        alimento.setStock(0);
        simularUsuario();
        when(productoRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(collar, alimento));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L, 20L)));

        assertTrue(error.getMessage().contains("no tiene stock disponible"));
        verify(ventaRepository, never()).save(any());
    }

    // ---------- Relaciones entre objetos (criterio 4) ----------

    @Test
    @DisplayName("La venta queda vinculada al usuario que compra (relacion Venta-Usuario)")
    void realizarVenta_datosValidos_ventaVinculadaAlUsuario() {
        simularUsuario();
        simularProductosYGuardado();

        Venta venta = ventaService.realizarVenta(1L, List.of(10L, 20L));

        assertEquals(usuario, venta.getUsuario());
        assertEquals(Rol.CLIENTE, venta.getUsuario().getRol());
    }

    @Test
    @DisplayName("La venta contiene los productos vendidos (relacion Venta-Producto)")
    void realizarVenta_datosValidos_ventaContieneLosProductos() {
        simularUsuario();
        simularProductosYGuardado();

        Venta venta = ventaService.realizarVenta(1L, List.of(10L, 20L));

        assertEquals(2, venta.getProductos().size());
        assertTrue(venta.getProductos().contains(collar));
        assertTrue(venta.getProductos().contains(alimento));
    }

    // ---------- Resultados de calculos (criterio 5) ----------

    @Test
    @DisplayName("El total de la venta es la suma de los precios de los productos")
    void realizarVenta_dosProductos_totalEsLaSumaDeLosPrecios() {
        simularUsuario();
        simularProductosYGuardado();

        Venta venta = ventaService.realizarVenta(1L, List.of(10L, 20L));

        // 5990 + 18990 = 24980
        assertEquals(new BigDecimal("24980"), venta.getTotal());
    }

    @Test
    @DisplayName("Cada producto vendido descuenta una unidad de su stock")
    void realizarVenta_dosProductos_descuentaElStock() {
        simularUsuario();
        simularProductosYGuardado();

        ventaService.realizarVenta(1L, List.of(10L, 20L));

        assertEquals(4, collar.getStock());
        assertEquals(2, alimento.getStock());
        verify(productoRepository).save(collar);
        verify(productoRepository).save(alimento);
    }
}