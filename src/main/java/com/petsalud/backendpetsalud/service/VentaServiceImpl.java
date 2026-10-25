package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.entity.Venta;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import com.petsalud.backendpetsalud.repository.ProductoRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.repository.VentaRepository;
import com.petsalud.backendpetsalud.service.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
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
        autorizacionService.verificarPropietarioOAdmin(usuarioId);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<Producto> productos = productoRepository.findAllById(productosIds);

        if (productos.size() != productosIds.size()) {
            throw new IllegalArgumentException("Algunos productos no fueron encontrados");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Producto producto : productos) {
            if (producto.getStock() <= 0) {
                throw new IllegalArgumentException("El producto " + producto.getNombre() + " no tiene stock disponible");
            }
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
}

