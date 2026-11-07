package com.petsalud.backendpetsalud.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsalud.backendpetsalud.controller.AuthController;
import com.petsalud.backendpetsalud.controller.ProductoController;
import com.petsalud.backendpetsalud.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, ProductoController.class})
@Import({SecurityConfig.class, JwtRequestFilter.class, JwtUtil.class, LoginAttemptService.class})
@TestPropertySource(properties = {"jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "jwt.expiration-ms=1800000", "jwt.issuer=PetSalud"})
class JwtLoginFlowTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private PasswordEncoder encoder;
    @MockBean private UsuarioDetailsService usuarios;
    @MockBean private ProductoService productos;

    @Test
    void loginGeneraJwtYPermiteAccesoProtegido() throws Exception {
        String email = "vet@petsalud.cl";
        String clave = "Clave1234";
        var usuario = User.withUsername(email).password(encoder.encode(clave)).authorities("VETERINARIO").build();
        when(usuarios.loadUserByUsername(email)).thenReturn(usuario);
        when(productos.listarProductosConStockBajo(5)).thenReturn(List.of());
        var respuesta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"vet@petsalud.cl\",\"password\":\"Clave1234\"}")).andExpect(status().isOk()).andReturn();
        String token = mapper.readTree(respuesta.getResponse().getContentAsString()).path("token").asText();
        assertFalse(token.isBlank());
        mvc.perform(get("/api/productos/stock-bajo").param("cantidad", "5").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        verify(productos).listarProductosConStockBajo(5);
    }
}
