package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Producto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Producto del catalogo (lo crea y modifica el gerente). */
public record ProductoRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE)
        String nombre,

        @Size(max = 500) @Pattern(regexp = Validaciones.TEXTO_SEGURO, message = Validaciones.MSG_TEXTO_SEGURO)
        String descripcion,

        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2)
        BigDecimal precio,

        @NotNull @PositiveOrZero
        Integer stock,

        @Size(max = 50) @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MSG_NOMBRE)
        String categoria
) {
    public Producto aEntidad() {
        return Producto.builder()
                .nombre(nombre).descripcion(descripcion).precio(precio)
                .stock(stock).categoria(categoria)
                .build();
    }
}
