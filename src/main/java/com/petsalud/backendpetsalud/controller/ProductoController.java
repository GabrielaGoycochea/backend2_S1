package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.dto.ProductoRequest;
import com.petsalud.backendpetsalud.dto.Validaciones;
import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.security.ReglasAcceso;
import com.petsalud.backendpetsalud.service.ProductoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @PreAuthorize(ReglasAcceso.GERENTE)
    @PostMapping
    public ResponseEntity<Producto> agregarProducto(@Valid @RequestBody ProductoRequest datos) {
        Producto nuevoProducto = productoService.agregarProducto(datos.aEntidad());
        return ResponseEntity.ok(nuevoProducto);
    }

    @PreAuthorize(ReglasAcceso.PUBLICO)
    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerProductoPorId(@PathVariable Long id) {
        return productoService.obtenerProductoPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize(ReglasAcceso.GERENTE)
    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizarProducto(@PathVariable Long id, @Valid @RequestBody ProductoRequest datos) {
        Producto productoActualizado = productoService.actualizarProducto(id, datos.aEntidad());
        return ResponseEntity.ok(productoActualizado);
    }

    @PreAuthorize(ReglasAcceso.GERENTE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(ReglasAcceso.PUBLICO)
    @GetMapping
    public ResponseEntity<List<Producto>> listarProductos() {
        List<Producto> productos = productoService.listarProductos();
        return ResponseEntity.ok(productos);
    }

    @PreAuthorize(ReglasAcceso.PUBLICO)
    @GetMapping("/categoria")
    public ResponseEntity<List<Producto>> listarProductosPorCategoria(
            @RequestParam @Size(max = 50) @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE) String categoria) {
        List<Producto> productos = productoService.listarProductosPorCategoria(categoria);
        return ResponseEntity.ok(productos);
    }

    @PreAuthorize(ReglasAcceso.PERSONAL)
    @GetMapping("/stock-bajo")
    public ResponseEntity<List<Producto>> listarProductosConStockBajo(@RequestParam Integer cantidad) {
        List<Producto> productos = productoService.listarProductosConStockBajo(cantidad);
        return ResponseEntity.ok(productos);
    }
}
