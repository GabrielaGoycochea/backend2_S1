package com.petsalud.backendpetsalud.controller;

import org.junit.jupiter.api.Tag;
import com.petsalud.backendpetsalud.security.ActividadSospechosaService;
import com.petsalud.backendpetsalud.security.JwtRequestFilter;
import com.petsalud.backendpetsalud.security.JwtUtil;
import com.petsalud.backendpetsalud.security.SecurityConfig;
import com.petsalud.backendpetsalud.security.UsuarioDetailsService;
import com.petsalud.backendpetsalud.service.VentaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Semana 5 (retroalimentación de la semana 4): 401 y 403 de /api/ventas.
 * VentaControllerTest usa standaloneSetup, que no pasa por Spring Security; por eso los permisos
 * se prueban aquí con @WebMvcTest y la configuración de seguridad real. Estructura AAA en cada prueba.
 */
@Tag("integration")
@WebMvcTest(VentaController.class)
@Import({SecurityConfig.class, JwtRequestFilter.class, ActividadSospechosaService.class})
@DisplayName("VentaController: permisos con seguridad real")
class VentaControllerSeguridadTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private VentaService ventaService;
    @MockBean
    private UsuarioDetailsService usuarioDetailsService;
    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("Sin token: 401 y el servicio no se llama")
    void realizarVenta_sinToken_responde401() throws Exception {
        // Act
        ResultActions respuesta = mvc.perform(post("/api/ventas").param("usuarioId", "1")
                .contentType(MediaType.APPLICATION_JSON).content("[10]"));

        // Assert
        respuesta.andExpect(status().isUnauthorized());
        verifyNoInteractions(ventaService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    @DisplayName("CLIENTE pide todas las ventas: 403 por @PreAuthorize y el servicio no se llama")
    void listarTodas_cliente_responde403() throws Exception {
        // Act
        ResultActions respuesta = mvc.perform(get("/api/ventas"));

        // Assert
        respuesta.andExpect(status().isForbidden());
        verifyNoInteractions(ventaService);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("ADMIN pide todas las ventas: 200")
    void listarTodas_admin_responde200() throws Exception {
        // Arrange
        when(ventaService.listarTodasLasVentas()).thenReturn(List.of());

        // Act
        ResultActions respuesta = mvc.perform(get("/api/ventas"));

        // Assert
        respuesta.andExpect(status().isOk());
        verify(ventaService).listarTodasLasVentas();
    }

    @Test
    @WithMockUser(authorities = "VETERINARIO")
    @DisplayName("VETERINARIO intenta vender: la regla de la semana 4 en el servicio responde 403")
    void realizarVenta_veterinario_responde403() throws Exception {
        // Arrange: VentaServiceImpl lanza AccessDeniedException para el rol VETERINARIO
        when(ventaService.realizarVenta(1L, List.of(10L)))
                .thenThrow(new AccessDeniedException("Solo usuarios con rol ADMIN o CLIENTE pueden realizar ventas"));

        // Act
        ResultActions respuesta = mvc.perform(post("/api/ventas").param("usuarioId", "1")
                .contentType(MediaType.APPLICATION_JSON).content("[10]"));

        // Assert
        respuesta.andExpect(status().isForbidden());
    }
}
