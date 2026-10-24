package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutorizacionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AutorizacionService autorizacionService;

    private void autenticar(String email) {
        var authentication = new UsernamePasswordAuthenticationToken(
                email, null, List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Usuario crearUsuario(Long id, String email, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setRol(rol);
        return usuario;
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void clientePuedeAccederARecursoPropio() {
        autenticar("cliente@petsalud.cl");

        when(usuarioRepository.findByEmail("cliente@petsalud.cl"))
                .thenReturn(Optional.of(
                        crearUsuario(3L, "cliente@petsalud.cl", Rol.CLIENTE)));

        assertDoesNotThrow(() ->
                autorizacionService.verificarPropietarioOAdmin(3L));
    }

    @Test
    void clienteNoPuedeAccederARecursoAjeno() {
        autenticar("cliente2@petsalud.cl");

        when(usuarioRepository.findByEmail("cliente2@petsalud.cl"))
                .thenReturn(Optional.of(
                        crearUsuario(4L, "cliente2@petsalud.cl", Rol.CLIENTE)));

        assertThrows(AccessDeniedException.class, () ->
                autorizacionService.verificarPropietarioOAdmin(3L));
    }

    @Test
    void administradorPuedeAccederARecursoDeOtroPropietario() {
        autenticar("admin@petsalud.cl");

        when(usuarioRepository.findByEmail("admin@petsalud.cl"))
                .thenReturn(Optional.of(
                        crearUsuario(1L, "admin@petsalud.cl", Rol.ADMIN)));

        assertDoesNotThrow(() ->
                autorizacionService.verificarPropietarioOAdmin(3L));
    }

    @Test
    void veterinarioNoPuedeRealizarOperacionReservada() {
        autenticar("vet@petsalud.cl");

        when(usuarioRepository.findByEmail("vet@petsalud.cl"))
                .thenReturn(Optional.of(
                        crearUsuario(2L, "vet@petsalud.cl", Rol.VETERINARIO)));

        assertThrows(AccessDeniedException.class, () ->
                autorizacionService.verificarPropietarioOAdmin(3L));
    }

    @Test
    void usuarioSinAutenticacionEsDenegado() {
        SecurityContextHolder.clearContext();

        assertThrows(AccessDeniedException.class, () ->
                autorizacionService.verificarPropietarioOAdmin(3L));

        verifyNoInteractions(usuarioRepository);
    }
}