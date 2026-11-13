package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Proveedor;
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
 * pruebas unitarias de las ventas.
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
        // Tampoco se descuenta el stock del producto que si tenia unidades
        verify(productoRepository, never()).save(any());
        assertEquals(5, collar.getStock());
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

    @Test
    @DisplayName("Un producto con stock nulo se trata como sin stock")
    void realizarVenta_stockNulo_lanzaError() {
        collar.setStock(null);
        simularUsuario();
        when(productoRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(collar, alimento));

        assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L, 20L)));

        verify(ventaRepository, never()).save(any());
    }

    // ---------- Relacion Producto-Proveedor ----------

    @Test
    @DisplayName("Cada producto de la venta sigue vinculado a su proveedor")
    void realizarVenta_datosValidos_productosConservanSuProveedor() {
        Proveedor vetSupply = Proveedor.builder().id(1L).nombre("VetSupply").build();
        Proveedor petFood = Proveedor.builder().id(2L).nombre("PetFood").build();
        collar.setProveedor(vetSupply);
        alimento.setProveedor(petFood);
        simularUsuario();
        simularProductosYGuardado();

        Venta venta = ventaService.realizarVenta(1L, List.of(10L, 20L));

        assertEquals(vetSupply, venta.getProductos().get(0).getProveedor());
        assertEquals(petFood, venta.getProductos().get(1).getProveedor());
    }

    // ---------- Consultas de ventas ----------

    @Test
    @DisplayName("Al consultar una venta se verifica que el usuario sea propietario o admin")
    void obtenerVentaPorId_existe_verificaPropietario() {
        Venta venta = Venta.builder().id(5L).usuario(usuario).build();
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));

        assertTrue(ventaService.obtenerVentaPorId(5L).isPresent());
        verify(autorizacionService).verificarPropietarioOAdmin(1L);
    }

    @Test
    @DisplayName("Consultar una venta inexistente devuelve vacio sin verificar propiedad")
    void obtenerVentaPorId_noExiste_devuelveVacio() {
        when(ventaRepository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(ventaService.obtenerVentaPorId(99L).isEmpty());
        verify(autorizacionService, never()).verificarPropietarioOAdmin(any());
    }

    @Test
    @DisplayName("Lista las ventas de un usuario verificando la propiedad")
    void listarVentasPorUsuario_devuelveLasVentasDelUsuario() {
        when(ventaRepository.findByUsuarioId(1L)).thenReturn(List.of(new Venta(), new Venta()));

        assertEquals(2, ventaService.listarVentasPorUsuario(1L).size());
        verify(autorizacionService).verificarPropietarioOAdmin(1L);
    }

    @Test
    @DisplayName("Un ADMIN puede listar todas las ventas")
    void listarTodasLasVentas_admin_devuelveTodas() {
        usuario.setRol(Rol.ADMIN);
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(ventaRepository.findAll()).thenReturn(List.of(new Venta()));

        assertEquals(1, ventaService.listarTodasLasVentas().size());
    }

    @Test
    @DisplayName("Un CLIENTE no puede listar todas las ventas")
    void listarTodasLasVentas_cliente_lanzaAccesoDenegado() {
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(usuario);

        assertThrows(AccessDeniedException.class, () -> ventaService.listarTodasLasVentas());
        verify(ventaRepository, never()).findAll();
    }
}
