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
import java.util.List;
import java.time.LocalDateTime;
import com.petsalud.backendpetsalud.entity.Rol;

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
        veterinario.setRol(Rol.VETERINARIO);

        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(veterinario));
        when(expedienteMedicoRepository.save(any(ExpedienteMedico.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        ExpedienteMedico registro = ExpedienteMedico.builder().descripcion("Control").diagnostico("Salud estable").tratamiento("Seguimiento").build();
        LocalDateTime inicio = LocalDateTime.now();
        ExpedienteMedico resultado = expedienteService.agregarRegistro(1L, 2L, registro);
        assertSame(mascota, resultado.getMascota());
        assertEquals("Control", resultado.getDescripcion());
        assertEquals("Salud estable", resultado.getDiagnostico());
        assertEquals("Seguimiento", resultado.getTratamiento());
        assertNotNull(resultado.getFecha());
        assertFalse(resultado.getFecha().isBefore(inicio));
        assertFalse(resultado.getFecha().isAfter(LocalDateTime.now()));
        verify(expedienteMedicoRepository).save(registro);

        assertSame(veterinario, resultado.getVeterinario());
        verify(autorizacionService).verificarIdentidadVeterinario(2L);
    }

    @Test
    void mascotaInexistente_rechazaRegistro() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> expedienteService.agregarRegistro(1L, 2L, new ExpedienteMedico()));
        verifyNoInteractions(expedienteMedicoRepository, usuarioRepository);
    }

    @Test
    void veterinarioInexistente_rechazaRegistro() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(new Mascota()));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> expedienteService.agregarRegistro(1L, 2L, new ExpedienteMedico()));
        verifyNoInteractions(expedienteMedicoRepository);
    }

    @Test
    void usuarioSinRolVeterinario_rechazaRegistro() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(new Mascota()));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(Usuario.builder().id(2L).rol(Rol.CLIENTE).build()));
        assertThrows(IllegalArgumentException.class, () -> expedienteService.agregarRegistro(1L, 2L, new ExpedienteMedico()));
        verifyNoInteractions(expedienteMedicoRepository);
    }

    @Test
    void obtenerRegistroExistente() {
        ExpedienteMedico registro = ExpedienteMedico.builder().id(1L).build();
        when(expedienteMedicoRepository.findById(1L)).thenReturn(Optional.of(registro));
        assertSame(registro, expedienteService.obtenerRegistroPorId(1L).orElseThrow());
    }

    @Test
    void obtenerRegistroInexistente() {
        when(expedienteMedicoRepository.findById(99L)).thenReturn(Optional.empty());
        assertTrue(expedienteService.obtenerRegistroPorId(99L).isEmpty());
    }

    @Test
    void listarRegistrosPorMascota() {
        List<ExpedienteMedico> registros = List.of(ExpedienteMedico.builder().id(1L).build(), ExpedienteMedico.builder().id(2L).build());
        when(expedienteMedicoRepository.findByMascotaId(3L)).thenReturn(registros);
        assertEquals(registros, expedienteService.listarRegistrosPorMascota(3L));
    }

    @Test
    void listarMascotaSinRegistros() {
        when(expedienteMedicoRepository.findByMascotaId(3L)).thenReturn(List.of());
        assertTrue(expedienteService.listarRegistrosPorMascota(3L).isEmpty());
    }

    @Test
    void eliminarRegistro() {
        expedienteService.eliminarRegistro(1L);
        verify(expedienteMedicoRepository).deleteById(1L);
    }
}
