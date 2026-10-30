package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.controller.ProductoController;
import com.petsalud.backendpetsalud.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductoController.class)
@Import({SecurityConfig.class, JwtRequestFilter.class})
class SecurityConfigHttpTest {
    @Autowired private MockMvc mvc;
    @MockBean private ProductoService productos;
    @MockBean private UsuarioDetailsService usuarios;
    @MockBean private JwtUtil jwtUtil;

    @Test
    void sinAutenticacionStockBajoResponde401() throws Exception {
        mvc.perform(get("/api/productos/stock-bajo").param("cantidad", "5")).andExpect(status().isUnauthorized());
        verifyNoInteractions(productos);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void clienteStockBajoResponde403() throws Exception {
        mvc.perform(get("/api/productos/stock-bajo").param("cantidad", "5")).andExpect(status().isForbidden());
        verifyNoInteractions(productos);
    }

    @Test
    @WithMockUser(authorities = "VETERINARIO")
    void veterinarioPuedeConsultarStockBajo() throws Exception {
        when(productos.listarProductosConStockBajo(5)).thenReturn(List.of());
        mvc.perform(get("/api/productos/stock-bajo").param("cantidad", "5")).andExpect(status().isOk());
        verify(productos).listarProductosConStockBajo(5);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void administradorPuedeConsultarStockBajo() throws Exception {
        when(productos.listarProductosConStockBajo(5)).thenReturn(List.of());
        mvc.perform(get("/api/productos/stock-bajo").param("cantidad", "5")).andExpect(status().isOk());
        verify(productos).listarProductosConStockBajo(5);
    }

    @Test
    void catalogoPublicoResponde200() throws Exception {
        when(productos.listarProductos()).thenReturn(List.of());
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
        verify(productos).listarProductos();
    }
}
