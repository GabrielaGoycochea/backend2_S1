package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.entity.Venta;
import com.petsalud.backendpetsalud.exception.GlobalExceptionHandler;
import com.petsalud.backendpetsalud.security.ActividadSospechosaService;
import com.petsalud.backendpetsalud.service.VentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Semana 4: pruebas del endpoint /api/ventas con MockMvc.
 * El servicio se simula con Mockito: se prueba solo el controlador (codigos HTTP y JSON).
 */
@ExtendWith(MockitoExtension.class)
class VentaControllerTest {

    @Mock
    private VentaService ventaService;

    @Mock
    private ActividadSospechosaService actividad;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        VentaController controlador = new VentaController();
        // El controlador usa @Autowired en el atributo: se le entrega el servicio simulado
        ReflectionTestUtils.setField(controlador, "ventaService", ventaService);

        mockMvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new GlobalExceptionHandler(actividad))
                .build();
    }

    @Test
    @DisplayName("POST /api/ventas responde 200 con el total y el usuario de la venta")
    void realizarVenta_datosValidos_responde200() throws Exception {
        // Preparar: el servicio simulado devuelve una venta ya registrada
        Usuario usuario = Usuario.builder().id(1L).nombre("Juan").rol(Rol.CLIENTE).build();
        Producto collar = Producto.builder().id(10L).nombre("Collar").precio(new BigDecimal("5990")).build();
        Venta venta = Venta.builder()
                .id(100L).usuario(usuario).productos(List.of(collar)).total(new BigDecimal("5990")).build();
        when(ventaService.realizarVenta(1L, List.of(10L))).thenReturn(venta);

        // Ejecutar y comprobar
        mockMvc.perform(post("/api/ventas")
                        .param("usuarioId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[10]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.total").value(5990))
                .andExpect(jsonPath("$.usuario.nombre").value("Juan"));
    }

    @Test
    @DisplayName("POST /api/ventas responde 400 si el usuario tiene datos incompletos")
    void realizarVenta_usuarioIncompleto_responde400() throws Exception {
        when(ventaService.realizarVenta(1L, List.of(10L)))
                .thenThrow(new IllegalArgumentException("El usuario no tiene direccion registrada"));

        mockMvc.perform(post("/api/ventas")
                        .param("usuarioId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[10]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El usuario no tiene direccion registrada"));
    }

    @Test
    @DisplayName("GET /api/ventas/{id} responde 404 si la venta no existe")
    void obtenerVentaPorId_noExiste_responde404() throws Exception {
        when(ventaService.obtenerVentaPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/ventas/99"))
                .andExpect(status().isNotFound());
    }
}
