package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de UsuarioServiceImpl.
 * La base de datos se simula con un mock de UsuarioRepository (sin MySQL).
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Usuario usuario;

    @BeforeEach
    void prepararDatos() {
        usuario = Usuario.builder()
                .id(1L)
                .email("juan.perez@example.com")
                .password("Clave1234")
                .nombre("Juan")
                .apellido("Perez")
                .direccion("Calle Falsa 123")
                .rol(Rol.CLIENTE)
                .build();
    }

    @Test
    @DisplayName("El usuario guardado en la base de datos tiene sus datos obligatorios completos")
    void obtenerUsuarioPorId_usuarioTieneDatosObligatoriosCompletos() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        Usuario encontrado = usuarioService.obtenerUsuarioPorId(1L).orElseThrow();

        assertNotNull(encontrado.getNombre());
        assertNotNull(encontrado.getApellido());
        assertNotNull(encontrado.getEmail());
        assertNotNull(encontrado.getDireccion());
    }

    @Test
    @DisplayName("Buscar por email delega en el repositorio")
    void obtenerUsuarioPorEmail_devuelveElUsuario() {
        when(usuarioRepository.findByEmail("juan.perez@example.com")).thenReturn(Optional.of(usuario));

        assertEquals(Optional.of(usuario), usuarioService.obtenerUsuarioPorEmail("juan.perez@example.com"));
    }

    @Test
    @DisplayName("Registrar un usuario cifra la clave y le asigna el rol CLIENTE")
    void registrarUsuario_cifraClaveYAsignaRolCliente() {
        usuario.setRol(Rol.ADMIN); // intenta auto-asignarse ADMIN
        when(usuarioRepository.findByEmail("juan.perez@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Clave1234")).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(llamada -> llamada.getArgument(0));

        Usuario guardado = usuarioService.registrarUsuario(usuario);

        assertEquals(Rol.CLIENTE, guardado.getRol());
        assertEquals("HASH", guardado.getPassword());
    }

    @Test
    @DisplayName("No registra un usuario con email repetido")
    void registrarUsuario_emailRepetido_lanzaError() {
        when(usuarioRepository.findByEmail("juan.perez@example.com")).thenReturn(Optional.of(usuario));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrarUsuario(usuario));

        assertEquals("El email ya está registrado", error.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar cambia los datos personales pero no el email ni la clave")
    void actualizarUsuario_noCambiaEmailNiClave() {
        Usuario cambios = Usuario.builder()
                .email("otro@example.com").password("otra")
                .nombre("Pedro").apellido("Soto").telefono("999").direccion("Calle Nueva 45").build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(llamada -> llamada.getArgument(0));

        Usuario actualizado = usuarioService.actualizarUsuario(1L, cambios);

        assertEquals("Pedro", actualizado.getNombre());
        assertEquals("Soto", actualizado.getApellido());
        assertEquals("999", actualizado.getTelefono());
        assertEquals("Calle Nueva 45", actualizado.getDireccion());
        assertEquals("juan.perez@example.com", actualizado.getEmail());
        assertEquals("Clave1234", actualizado.getPassword());
    }

    @Test
    @DisplayName("Actualizar un usuario inexistente lanza error")
    void actualizarUsuario_noExiste_lanzaError() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> usuarioService.actualizarUsuario(9L, new Usuario()));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Eliminar y listar usuarios delegan en el repositorio")
    void eliminarYListarUsuarios() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        usuarioService.eliminarUsuario(1L);

        verify(usuarioRepository).deleteById(1L);
        assertEquals(1, usuarioService.listarUsuarios().size());
    }
}
