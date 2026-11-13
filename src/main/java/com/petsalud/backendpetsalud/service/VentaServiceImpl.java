package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.entity.Venta;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import com.petsalud.backendpetsalud.repository.ProductoRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.repository.VentaRepository;
import com.petsalud.backendpetsalud.service.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VentaServiceImpl implements VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private AutorizacionService autorizacionService;

    @Override
    public Venta realizarVenta(Long usuarioId, List<Long> productosIds) {
        autorizacionService.verificarPersonalOPropietario(usuarioId);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        //antes de vender se validan los datos y el rol del usuario
        validarDatosCompletos(usuario);
        validarRolAutorizado(usuario);

        List<Producto> productos = productoRepository.findAllById(productosIds);

        if (productos.size() != productosIds.size()) {
            throw new IllegalArgumentException("Algunos productos no fueron encontrados");
        }

        // Primero se valida el stock de TODOS los productos; solo si hay stock para todos se descuenta
        for (Producto producto : productos) {
            if (producto.getStock() == null || producto.getStock() <= 0) {
                throw new IllegalArgumentException("El producto " + producto.getNombre() + " no tiene stock disponible");
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Producto producto : productos) {
            // Restar stock
            producto.setStock(producto.getStock() - 1);
            productoRepository.save(producto);

            total = total.add(producto.getPrecio());
        }

        Venta venta = Venta.builder()
                .usuario(usuario)
                .productos(productos)
                .total(total)
                .fecha(LocalDateTime.now())
                .build();

        return ventaRepository.save(venta);
    }

    @Override
    public Optional<Venta> obtenerVentaPorId(Long id) {
        Optional<Venta> resultado = ventaRepository.findById(id);

        resultado.ifPresent(venta ->
                autorizacionService.verificarPropietarioOAdmin(
                        venta.getUsuario().getId()
                )
        );

        return resultado;
    }

    @Override
    public List<Venta> listarVentasPorUsuario(Long usuarioId) {
        autorizacionService.verificarPropietarioOAdmin(usuarioId);
        return ventaRepository.findByUsuarioId(usuarioId);
    }

    @Override
    public List<Venta> listarTodasLasVentas() {
        Usuario usuario = autorizacionService.obtenerUsuarioAutenticado();

        if (usuario.getRol() != com.petsalud.backendpetsalud.entity.Rol.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo un administrador puede consultar todas las ventas");
        }

        return ventaRepository.findAll();
    }

    // El usuario de la venta debe tener nombre, apellido, email y direccion
    private void validarDatosCompletos(Usuario usuario) {
        if (estaVacio(usuario.getNombre())) {
            throw new IllegalArgumentException("El usuario no tiene nombre registrado");
        }
        if (estaVacio(usuario.getApellido())) {
            throw new IllegalArgumentException("El usuario no tiene apellido registrado");
        }
        if (estaVacio(usuario.getEmail())) {
            throw new IllegalArgumentException("El usuario no tiene email registrado");
        }
        if (estaVacio(usuario.getDireccion())) {
            throw new IllegalArgumentException("El usuario no tiene direccion registrada");
        }
    }

    // Solo los roles ADMIN y CLIENTE pueden realizar ventas
    private void validarRolAutorizado(Usuario usuario) {
        if (usuario.getRol() != Rol.ADMIN && usuario.getRol() != Rol.CLIENTE) {
            throw new AccessDeniedException("Solo usuarios con rol ADMIN o CLIENTE pueden realizar ventas");
        }
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}