package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.ExpedienteMedico;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.ExpedienteMedicoRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
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

/** Un veterinario solo puede registrar expedientes a su propio nombre. */
@ExtendWith(MockitoExtension.class)
class ExpedienteMedicoServiceImplSecurityTest {

    @Mock
    private ExpedienteMedicoRepository expedienteMedicoRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AutorizacionService autorizacionService;

    @InjectMocks
    private ExpedienteMedicoServiceImpl expedienteService;

    @Test
    void veterinarioNoPuedeRegistrarExpedienteConIdentidadAjena() {
        doThrow(new AccessDeniedException("Acceso denegado"))
                .when(autorizacionService).verificarIdentidadVeterinario(5L);

        assertThrows(AccessDeniedException.class,
                () -> expedienteService.agregarRegistro(1L, 5L, new ExpedienteMedico()));

        verify(expedienteMedicoRepository, never()).save(any(ExpedienteMedico.class));
    }

    @Test
    void veterinarioPuedeRegistrarExpedienteConSuPropiaIdentidad() {
        Mascota mascota = new Mascota();
        mascota.setId(1L);
        Usuario veterinario = new Usuario();
        veterinario.setId(2L);

        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(veterinario));
        when(expedienteMedicoRepository.save(any(ExpedienteMedico.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        ExpedienteMedico resultado = expedienteService.agregarRegistro(1L, 2L, new ExpedienteMedico());

        assertSame(veterinario, resultado.getVeterinario());
        verify(autorizacionService).verificarIdentidadVeterinario(2L);
    }
}
