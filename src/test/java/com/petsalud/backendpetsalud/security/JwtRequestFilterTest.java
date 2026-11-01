package com.petsalud.backendpetsalud.security;

import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtRequestFilterTest {
    private JwtUtil jwtUtil;
    private UsuarioDetailsService usuarios;
    private JwtRequestFilter filtro;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void preparar() {
        SecurityContextHolder.clearContext();
        jwtUtil = mock(JwtUtil.class);
        usuarios = mock(UsuarioDetailsService.class);
        filtro = new JwtRequestFilter(jwtUtil, usuarios);
        request = new MockHttpServletRequest("GET", "/api/mascotas/1");
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinTokenNoAutentica() throws Exception {
        filtro.doFilter(request, response, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtil, usuarios);
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenValidoAutenticaConRolActual() throws Exception {
        UserDetails usuario = User.withUsername("cliente@petsalud.cl").password("prueba").authorities("CLIENTE").build();
        request.addHeader("Authorization", "Bearer valido");
        when(jwtUtil.extraerUsername("valido")).thenReturn(usuario.getUsername());
        when(usuarios.loadUserByUsername(usuario.getUsername())).thenReturn(usuario);
        when(jwtUtil.validarToken("valido", usuario)).thenReturn(true);
        filtro.doFilter(request, response, chain);
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(autenticacion);
        assertEquals(usuario, autenticacion.getPrincipal());
        assertEquals("CLIENTE", autenticacion.getAuthorities().iterator().next().getAuthority());
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenMalformadoNoAutentica() throws Exception {
        request.addHeader("Authorization", "Bearer invalido");
        when(jwtUtil.extraerUsername("invalido")).thenThrow(new MalformedJwtException("Token malformado"));
        filtro.doFilter(request, response, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(usuarios);
        verify(chain).doFilter(request, response);
    }

    @Test
    void usuarioInexistenteNoAutentica() throws Exception {
        request.addHeader("Authorization", "Bearer desconocido");
        when(jwtUtil.extraerUsername("desconocido")).thenReturn("inexistente@petsalud.cl");
        when(usuarios.loadUserByUsername("inexistente@petsalud.cl")).thenThrow(new UsernameNotFoundException("Usuario inexistente"));
        filtro.doFilter(request, response, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenDeOtroUsuarioNoAutentica() throws Exception {
        UserDetails usuario = User.withUsername("cliente@petsalud.cl").password("prueba").authorities("CLIENTE").build();
        request.addHeader("Authorization", "Bearer ajeno");
        when(jwtUtil.extraerUsername("ajeno")).thenReturn(usuario.getUsername());
        when(usuarios.loadUserByUsername(usuario.getUsername())).thenReturn(usuario);
        when(jwtUtil.validarToken("ajeno", usuario)).thenReturn(false);
        filtro.doFilter(request, response, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}
