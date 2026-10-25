package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Recompensa;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.RecompensaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Las recompensas solo las consulta su dueno o el personal; crear y eliminar es solo del ADMIN. */
@ExtendWith(MockitoExtension.class)
class RecompensaServiceImplSecurityTest {

    @Mock
    private RecompensaRepository recompensaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AutorizacionService autorizacionService;

    @InjectMocks
    private RecompensaServiceImpl recompensaService;

    @Test
    void clienteNoPuedeConsultarRecompensaAjena() {
        Usuario propietario = new Usuario();
        propietario.setId(3L);
        Recompensa recompensa = new Recompensa();
        recompensa.setId(1L);
        recompensa.setUsuario(propietario);

        when(recompensaRepository.findById(1L)).thenReturn(Optional.of(recompensa));
        doThrow(new AccessDeniedException("Acceso denegado"))
                .when(autorizacionService).verificarPersonalOPropietario(3L);

        assertThrows(AccessDeniedException.class,
                () -> recompensaService.obtenerRecompensaPorId(1L));
    }

    @Test
    void clienteNoPuedeListarRecompensasAjenas() {
        doThrow(new AccessDeniedException("Acceso denegado"))
                .when(autorizacionService).verificarPersonalOPropietario(3L);

        assertThrows(AccessDeniedException.class,
                () -> recompensaService.listarRecompensasPorUsuario(3L));

        verify(recompensaRepository, never()).findByUsuarioId(anyLong());
    }

    @Test
    void soloElAdministradorPuedeAgregarRecompensas() {
        doThrow(new AccessDeniedException("Acceso denegado"))
                .when(autorizacionService).verificarAdmin();

        assertThrows(AccessDeniedException.class,
                () -> recompensaService.agregarRecompensa(3L, new Recompensa()));

        verify(recompensaRepository, never()).save(any(Recompensa.class));
    }

    @Test
    void soloElAdministradorPuedeEliminarRecompensas() {
        doThrow(new AccessDeniedException("Acceso denegado"))
                .when(autorizacionService).verificarAdmin();

        assertThrows(AccessDeniedException.class,
                () -> recompensaService.eliminarRecompensa(1L));

        verify(recompensaRepository, never()).deleteById(anyLong());
    }
}
