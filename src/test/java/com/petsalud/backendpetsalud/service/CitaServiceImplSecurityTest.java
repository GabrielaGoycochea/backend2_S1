package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.CitaRepository;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaServiceImplSecurityTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AutorizacionService autorizacionService;

    @InjectMocks
    private CitaServiceImpl citaService;

    private Mascota mascota;
    private Usuario veterinario;
    private LocalDateTime fecha;

    @BeforeEach
    void prepararDatos() {
        Usuario propietario = new Usuario();
        propietario.setId(3L);

        mascota = new Mascota();
        mascota.setId(1L);
        mascota.setPropietario(propietario);

        veterinario = new Usuario();
        veterinario.setId(5L);

        fecha = LocalDateTime.of(2026, 11, 10, 10, 0);
    }

    @Test
    void usuarioAutorizadoPuedeAgendarCita() {
        when(mascotaRepository.findById(1L))
                .thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(5L))
                .thenReturn(Optional.of(veterinario));
        when(citaRepository.save(any(Cita.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Cita resultado = citaService.agendarCita(
                1L, 5L, fecha, "Control veterinario");

        assertNotNull(resultado);
        assertEquals("Agendada", resultado.getEstado());
        assertEquals(mascota, resultado.getMascota());

        verify(autorizacionService)
                .verificarPropietarioOAdmin(3L);
        verify(citaRepository).save(any(Cita.class));
    }

    @Test
    void usuarioNoAutorizadoNoPuedeAgendarCita() {
        when(mascotaRepository.findById(1L))
                .thenReturn(Optional.of(mascota));

        doThrow(new AccessDeniedException("Acceso denegado"))
                .when(autorizacionService)
                .verificarPropietarioOAdmin(3L);

        assertThrows(AccessDeniedException.class,
                () -> citaService.agendarCita(
                        1L, 5L, fecha, "Control veterinario"));

        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    void clienteNoPuedeConsultarCitaAjena() {
        Cita cita = new Cita();
        cita.setId(1L);
        cita.setMascota(mascota);

        Usuario clienteAjeno = new Usuario();
        clienteAjeno.setId(5L);
        clienteAjeno.setRol(com.petsalud.backendpetsalud.entity.Rol.CLIENTE);

        when(citaRepository.findById(1L))
                .thenReturn(Optional.of(cita));

        when(autorizacionService.obtenerUsuarioAutenticado())
                .thenReturn(clienteAjeno);

        assertThrows(AccessDeniedException.class,
                () -> citaService.obtenerCitaPorId(1L));
    }
}
