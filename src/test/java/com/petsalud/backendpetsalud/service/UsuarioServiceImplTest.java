package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UsuarioServiceImpl usuarioService;

    private Usuario nuevoUsuario() {
        return Usuario.builder().email("nuevo@petsalud.cl").password("clave123").nombre("Ana").apellido("Diaz").direccion("Calle 123").rol(Rol.ADMIN).build();
    }

    @Test
    @DisplayName("Registro publico asigna CLIENTE y codifica la contrasena")
    void registrarUsuario_datosValidos_asignaClienteYCodificaPassword() {
        Usuario usuario = nuevoUsuario();
        when(usuarioRepository.findByEmail("nuevo@petsalud.cl")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("clave123")).thenReturn("hash_simulado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Usuario resultado = usuarioService.registrarUsuario(usuario);

        assertSame(usuario, resultado);
        assertEquals(Rol.CLIENTE, resultado.getRol());
        assertEquals("hash_simulado", resultado.getPassword());
        verify(passwordEncoder).encode("clave123");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("No registra un usuario si el correo ya existe")
    void registrarUsuario_emailDuplicado_rechazaRegistro() {
        Usuario usuario = nuevoUsuario();
        when(usuarioRepository.findByEmail("nuevo@petsalud.cl")).thenReturn(Optional.of(Usuario.builder().build()));

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Busca correctamente un usuario existente por ID")
    void obtenerUsuarioPorId_existente_devuelveUsuario() {
        Usuario usuario = nuevoUsuario();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        assertSame(usuario, usuarioService.obtenerUsuarioPorId(7L).orElseThrow());
        verify(usuarioRepository).findById(7L);
    }

    @Test
    @DisplayName("Devuelve Optional vacio si el ID no existe")
    void obtenerUsuarioPorId_inexistente_devuelveVacio() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        assertTrue(usuarioService.obtenerUsuarioPorId(99L).isEmpty());
        verify(usuarioRepository).findById(99L);
    }

    @Test
    @DisplayName("Busca correctamente un usuario existente por correo")
    void obtenerUsuarioPorEmail_existente_devuelveUsuario() {
        Usuario usuario = nuevoUsuario();
        when(usuarioRepository.findByEmail("nuevo@petsalud.cl")).thenReturn(Optional.of(usuario));
        assertSame(usuario, usuarioService.obtenerUsuarioPorEmail("nuevo@petsalud.cl").orElseThrow());
        verify(usuarioRepository).findByEmail("nuevo@petsalud.cl");
    }

    @Test
    @DisplayName("Devuelve Optional vacio si el correo no existe")
    void obtenerUsuarioPorEmail_inexistente_devuelveVacio() {
        when(usuarioRepository.findByEmail("ausente@petsalud.cl")).thenReturn(Optional.empty());
        assertTrue(usuarioService.obtenerUsuarioPorEmail("ausente@petsalud.cl").isEmpty());
        verify(usuarioRepository).findByEmail("ausente@petsalud.cl");
    }

    @Test
    @DisplayName("Actualiza datos personales sin modificar email, password ni rol")
    void actualizarUsuario_existente_actualizaSoloDatosPermitidos() {
        Usuario existente = Usuario.builder().id(7L).email("original@petsalud.cl").password("hash_original").nombre("Ana").apellido("Diaz").telefono("111111").direccion("Antigua 123").rol(Rol.ADMIN).build();
        Usuario cambios = Usuario.builder().email("nuevo@petsalud.cl").password("clave_nueva").nombre("Beatriz").apellido("Rojas").telefono("222222").direccion("Nueva 456").rol(Rol.CLIENTE).build();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Usuario resultado = usuarioService.actualizarUsuario(7L, cambios);

        assertSame(existente, resultado);
        assertEquals("Beatriz", resultado.getNombre());
        assertEquals("Rojas", resultado.getApellido());
        assertEquals("222222", resultado.getTelefono());
        assertEquals("Nueva 456", resultado.getDireccion());
        assertEquals("original@petsalud.cl", resultado.getEmail());
        assertEquals("hash_original", resultado.getPassword());
        assertEquals(Rol.ADMIN, resultado.getRol());
        verify(usuarioRepository).findById(7L);
        verify(usuarioRepository).save(existente);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("No actualiza ni guarda un usuario inexistente")
    void actualizarUsuario_inexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> usuarioService.actualizarUsuario(99L, nuevoUsuario()));

        assertEquals("Usuario no encontrado", error.getMessage());
        verify(usuarioRepository).findById(99L);
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Eliminar usuario delega la eliminacion al repositorio")
    void eliminarUsuario_idValido_invocaRepositorio() {
        usuarioService.eliminarUsuario(7L);
        verify(usuarioRepository).deleteById(7L);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Lista todos los usuarios existentes")
    void listarUsuarios_conRegistros_devuelveLista() {
        Usuario primero = nuevoUsuario();
        Usuario segundo = Usuario.builder().email("segundo@petsalud.cl").build();
        java.util.List<Usuario> usuarios = java.util.List.of(primero, segundo);
        when(usuarioRepository.findAll()).thenReturn(usuarios);

        java.util.List<Usuario> resultado = usuarioService.listarUsuarios();

        assertEquals(2, resultado.size());
        assertSame(usuarios, resultado);
        verify(usuarioRepository).findAll();
    }

    @Test
    @DisplayName("Devuelve lista vacia cuando no hay usuarios")
    void listarUsuarios_sinRegistros_devuelveListaVacia() {
        when(usuarioRepository.findAll()).thenReturn(java.util.List.of());

        java.util.List<Usuario> resultado = usuarioService.listarUsuarios();

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(usuarioRepository).findAll();
    }
}
