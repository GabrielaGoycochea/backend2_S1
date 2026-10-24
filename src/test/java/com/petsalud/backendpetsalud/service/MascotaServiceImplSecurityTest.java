package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MascotaServiceImplSecurityTest {

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AutorizacionService autorizacionService;

    @InjectMocks
    private MascotaServiceImpl mascotaService;

    private Mascota luna;

    @BeforeEach
    void prepararDatos() {
        Usuario propietario = new Usuario();
        propietario.setId(3L);

        luna = new Mascota();
        luna.setId(1L);
        luna.setNombre("Luna");
        luna.setPropietario(propietario);
    }

    @Test
    void propietarioPuedeConsultarSuMascota() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(luna));

        Optional<Mascota> resultado = mascotaService.obtenerMascotaPorId(1L);

        assertTrue(resultado.isPresent());
        assertEquals("Luna", resultado.get().getNombre());
        verify(autorizacionService).verificarPropietarioOAdmin(3L);
    }

    @Test
    void clienteNoPuedeConsultarMascotaAjena() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(luna));
        doThrow(new AccessDeniedException("Acceso denegado"))
                .when(autorizacionService).verificarPropietarioOAdmin(3L);

        assertThrows(AccessDeniedException.class,
                () -> mascotaService.obtenerMascotaPorId(1L));
    }

    @Test
    void clienteNoPuedeModificarMascotaAjena() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(luna));
        doThrow(new AccessDeniedException("Acceso denegado"))
                .when(autorizacionService).verificarPropietarioOAdmin(3L);

        Mascota cambios = new Mascota();
        cambios.setNombre("CambioNoAutorizado");

        assertThrows(AccessDeniedException.class,
                () -> mascotaService.actualizarMascota(1L, cambios));

        verify(mascotaRepository, never()).save(any(Mascota.class));
        assertEquals("Luna", luna.getNombre());
    }

    @Test
    void clienteNoPuedeEliminarMascotaAjena() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(luna));
        doThrow(new AccessDeniedException("Acceso denegado"))
                .when(autorizacionService).verificarPropietarioOAdmin(3L);

        assertThrows(AccessDeniedException.class,
                () -> mascotaService.eliminarMascota(1L));

        verify(mascotaRepository, never()).delete(any(Mascota.class));
    }

    @Test
    void clienteNoPuedeRegistrarMascotaParaOtroPropietario() {
        doThrow(new AccessDeniedException("Acceso denegado"))
                .when(autorizacionService).verificarPropietarioOAdmin(3L);

        assertThrows(AccessDeniedException.class,
                () -> mascotaService.registrarMascota(new Mascota(), 3L));

        verify(mascotaRepository, never()).save(any(Mascota.class));
    }

    @Test
    void clienteNoPuedeListarMascotasDeOtroPropietario() {
        doThrow(new AccessDeniedException("Acceso denegado"))
                .when(autorizacionService).verificarPropietarioOAdmin(3L);

        assertThrows(AccessDeniedException.class,
                () -> mascotaService.listarMascotasPorPropietario(3L));

        verify(mascotaRepository, never()).findByPropietarioId(anyLong());
    }
}