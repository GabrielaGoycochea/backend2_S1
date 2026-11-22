package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.CitaRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * S5: pruebas unitarias de las citas (creacion, disponibilidad, estado, cancelacion
 * y relacion mascota-propietario-veterinario).
 * Los repositorios y la autorizacion se simulan con Mockito (no se usa MySQL).
 */
@ExtendWith(MockitoExtension.class)
class CitaServiceImplTest {

    @Mock private CitaRepository citaRepository;
    @Mock private MascotaRepository mascotaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AutorizacionService autorizacionService;

    @InjectMocks
    private CitaServiceImpl citaService;

    private Usuario propietario;
    private Usuario veterinario;
    private Usuario admin;
    private Mascota mascota;
    private LocalDateTime fecha;

    @BeforeEach
    void prepararDatos() {
        propietario = Usuario.builder().id(3L).email("cliente@petsalud.cl").nombre("Ana").rol(Rol.CLIENTE).build();
        veterinario = Usuario.builder().id(5L).email("vet@petsalud.cl").nombre("Luis").rol(Rol.VETERINARIO).build();
        admin = Usuario.builder().id(1L).email("admin@petsalud.cl").nombre("Admin").rol(Rol.ADMIN).build();
        mascota = Mascota.builder().id(1L).nombre("Firulais").especie("Perro").propietario(propietario).build();
        fecha = LocalDateTime.of(2030, 1, 15, 10, 0); // fecha futura fija: resultados reproducibles
    }

    private void simularAgendamientoValido() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(veterinario));
        when(citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNot(5L, fecha, "Cancelada")).thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Cita cita(Long id, Usuario vet) {
        return Cita.builder().id(id).mascota(mascota).veterinario(vet).fechaHora(fecha).estado("Agendada").build();
    }

    // ---------- Creacion de citas ----------

    @Test
    @DisplayName("Una cita agendada correctamente queda con estado inicial Agendada")
    void agendarCita_datosValidos_estadoInicialAgendada() {
        simularAgendamientoValido();

        Cita resultado = citaService.agendarCita(1L, 5L, fecha, "Control anual");

        assertEquals("Agendada", resultado.getEstado());
        assertEquals("Control anual", resultado.getMotivo());
        assertEquals(fecha, resultado.getFechaHora());
        verify(citaRepository).save(any(Cita.class));
    }

    @Test
    @DisplayName("Antes de agendar se consulta la disponibilidad del veterinario en esa fecha y hora")
    void agendarCita_datosValidos_consultaDisponibilidadDelVeterinario() {
        simularAgendamientoValido();

        citaService.agendarCita(1L, 5L, fecha, "Control anual");

        verify(citaRepository).existsByVeterinarioIdAndFechaHoraAndEstadoNot(5L, fecha, "Cancelada");
    }

    @Test
    @DisplayName("No se puede agendar una cita en un horario ya ocupado por el veterinario")
    void agendarCita_horarioOcupado_lanzaError() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(veterinario));
        when(citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNot(5L, fecha, "Cancelada")).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.agendarCita(1L, 5L, fecha, "Control anual"));

        assertTrue(error.getMessage().contains("horario"));
        verify(citaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una mascota inexistente impide agendar la cita")
    void agendarCita_mascotaInexistente_lanzaError() {
        when(mascotaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> citaService.agendarCita(99L, 5L, fecha, "Control"));

        verify(citaRepository, never()).save(any());
    }

    // ---------- Relacion mascota-veterinario ----------

    @Test
    @DisplayName("El veterinario y la mascota de la cita son los indicados")
    void agendarCita_datosValidos_veterinarioYMascotaAsignadosCorrectamente() {
        simularAgendamientoValido();

        Cita resultado = citaService.agendarCita(1L, 5L, fecha, "Control anual");

        assertSame(veterinario, resultado.getVeterinario());
        assertEquals(5L, resultado.getVeterinario().getId());
        assertSame(mascota, resultado.getMascota());
    }

    @Test
    @DisplayName("Al agendar se verifica que el usuario autenticado sea el propietario de la mascota o un ADMIN")
    void agendarCita_datosValidos_verificaAutorizacionDelPropietario() {
        simularAgendamientoValido();

        citaService.agendarCita(1L, 5L, fecha, "Control anual");

        verify(autorizacionService).verificarPropietarioOAdmin(3L);
    }

    @Test
    @DisplayName("Una mascota sin propietario no puede tener citas")
    void agendarCita_mascotaSinPropietario_lanzaError() {
        mascota.setPropietario(null);
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.agendarCita(1L, 5L, fecha, "Control"));

        assertTrue(error.getMessage().contains("propietario"));
        verifyNoInteractions(autorizacionService);
        verify(citaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un veterinario inexistente impide agendar la cita")
    void agendarCita_veterinarioInexistente_lanzaError() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> citaService.agendarCita(1L, 5L, fecha, "Control"));

        verify(citaRepository, never()).save(any());
    }

    @ParameterizedTest(name = "Un usuario con rol {0} no puede ser asignado como veterinario")
    @EnumSource(value = Rol.class, names = {"CLIENTE", "ADMIN"})
    void agendarCita_usuarioQueNoEsVeterinario_lanzaError(Rol rol) {
        veterinario.setRol(rol);
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(veterinario));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.agendarCita(1L, 5L, fecha, "Control"));

        assertTrue(error.getMessage().contains("veterinario"));
        verify(citaRepository, never()).save(any());
    }

    // ---------- Consulta de una cita ----------

    @Test
    @DisplayName("Consultar una cita inexistente devuelve vacio")
    void obtenerCitaPorId_noExiste_devuelveVacio() {
        when(citaRepository.findById(9L)).thenReturn(Optional.empty());

        assertTrue(citaService.obtenerCitaPorId(9L).isEmpty());
        verifyNoInteractions(autorizacionService);
    }

    @Test
    @DisplayName("El ADMIN puede consultar cualquier cita")
    void obtenerCitaPorId_admin_devuelveLaCita() {
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita(1L, veterinario)));
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(admin);

        assertTrue(citaService.obtenerCitaPorId(1L).isPresent());
    }

    @Test
    @DisplayName("El CLIENTE propietario de la mascota puede consultar su cita")
    void obtenerCitaPorId_propietario_devuelveLaCita() {
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita(1L, veterinario)));
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(propietario);

        assertTrue(citaService.obtenerCitaPorId(1L).isPresent());
    }

    @Test
    @DisplayName("El veterinario asignado puede consultar la cita")
    void obtenerCitaPorId_veterinarioAsignado_devuelveLaCita() {
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita(1L, veterinario)));
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(veterinario);

        assertTrue(citaService.obtenerCitaPorId(1L).isPresent());
    }

    @Test
    @DisplayName("Un veterinario que no esta asignado no puede consultar la cita")
    void obtenerCitaPorId_veterinarioNoAsignado_lanzaAccesoDenegado() {
        Usuario otroVeterinario = Usuario.builder().id(6L).rol(Rol.VETERINARIO).build();
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita(1L, veterinario)));
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(otroVeterinario);

        assertThrows(AccessDeniedException.class, () -> citaService.obtenerCitaPorId(1L));
    }

    // ---------- Estado de la cita ----------

    @Test
    @DisplayName("El ADMIN puede cambiar el estado de una cita")
    void actualizarEstadoCita_admin_cambiaElEstado() {
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita(1L, veterinario)));
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(admin);
        when(citaRepository.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

        Cita resultado = citaService.actualizarEstadoCita(1L, "Completada");

        assertEquals("Completada", resultado.getEstado());
        verify(citaRepository).save(any(Cita.class));
    }

    @Test
    @DisplayName("El veterinario asignado puede cambiar el estado de su cita")
    void actualizarEstadoCita_veterinarioAsignado_cambiaElEstado() {
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita(1L, veterinario)));
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(veterinario);
        when(citaRepository.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("Cancelada", citaService.actualizarEstadoCita(1L, "Cancelada").getEstado());
    }

    @Test
    @DisplayName("Un CLIENTE no puede cambiar el estado de una cita")
    void actualizarEstadoCita_cliente_lanzaAccesoDenegado() {
        Cita existente = cita(1L, veterinario);
        when(citaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(propietario);

        assertThrows(AccessDeniedException.class, () -> citaService.actualizarEstadoCita(1L, "Completada"));

        assertEquals("Agendada", existente.getEstado());
        verify(citaRepository, never()).save(any());
    }

    @Test
    @DisplayName("No se puede cambiar el estado de una cita inexistente")
    void actualizarEstadoCita_inexistente_lanzaError() {
        when(citaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> citaService.actualizarEstadoCita(9L, "Completada"));
    }

    // ---------- Cancelacion ----------

    @Test
    @DisplayName("El propietario puede cancelar la cita y esta se elimina")
    void cancelarCita_propietario_eliminaLaCita() {
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita(1L, veterinario)));

        citaService.cancelarCita(1L);

        verify(autorizacionService).verificarPropietarioOAdmin(3L);
        verify(citaRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Cancelar una cita cuya mascota no tiene propietario lanza un error claro")
    void cancelarCita_mascotaSinPropietario_lanzaError() {
        mascota.setPropietario(null);
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita(1L, veterinario)));

        assertThrows(IllegalArgumentException.class, () -> citaService.cancelarCita(1L));

        verifyNoInteractions(autorizacionService);
        verify(citaRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Cancelar una cita sin mascota asociada lanza un error claro")
    void cancelarCita_citaSinMascota_lanzaError() {
        Cita sinMascota = Cita.builder().id(1L).veterinario(veterinario).fechaHora(fecha).estado("Agendada").build();
        when(citaRepository.findById(1L)).thenReturn(Optional.of(sinMascota));

        assertThrows(IllegalArgumentException.class, () -> citaService.cancelarCita(1L));

        verify(citaRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("No se puede cancelar una cita inexistente")
    void cancelarCita_inexistente_lanzaError() {
        when(citaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> citaService.cancelarCita(9L));

        verify(citaRepository, never()).deleteById(any());
    }

    // ---------- Listados ----------

    @Test
    @DisplayName("El ADMIN lista todas las citas de una mascota")
    void listarCitasPorMascota_admin_devuelveTodas() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(admin);
        when(citaRepository.findByMascotaId(1L)).thenReturn(List.of(cita(1L, veterinario), cita(2L, veterinario)));

        assertEquals(2, citaService.listarCitasPorMascota(1L).size());
    }

    @Test
    @DisplayName("El CLIENTE lista las citas de su mascota verificando la propiedad")
    void listarCitasPorMascota_cliente_verificaPropietario() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(propietario);
        when(citaRepository.findByMascotaId(1L)).thenReturn(List.of(cita(1L, veterinario)));

        assertEquals(1, citaService.listarCitasPorMascota(1L).size());
        verify(autorizacionService).verificarPropietarioOAdmin(3L);
    }

    @Test
    @DisplayName("El veterinario solo ve las citas de la mascota que tiene asignadas")
    void listarCitasPorMascota_veterinario_soloVeSusCitas() {
        Usuario otroVeterinario = Usuario.builder().id(6L).rol(Rol.VETERINARIO).build();
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(veterinario);
        when(citaRepository.findByMascotaId(1L))
                .thenReturn(List.of(cita(1L, veterinario), cita(2L, otroVeterinario), cita(3L, null)));

        List<Cita> resultado = citaService.listarCitasPorMascota(1L);

        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getId());
    }

    @Test
    @DisplayName("Un usuario sin rol no puede listar citas de una mascota")
    void listarCitasPorMascota_usuarioSinRol_lanzaAccesoDenegado() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(new Usuario());

        assertThrows(AccessDeniedException.class, () -> citaService.listarCitasPorMascota(1L));
    }

    @Test
    @DisplayName("Listar citas de una mascota inexistente lanza error")
    void listarCitasPorMascota_mascotaInexistente_lanzaError() {
        when(mascotaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> citaService.listarCitasPorMascota(99L));
    }

    @Test
    @DisplayName("El ADMIN puede listar las citas de cualquier veterinario")
    void listarCitasPorVeterinario_admin_devuelveLasCitas() {
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(admin);
        when(citaRepository.findByVeterinarioId(5L)).thenReturn(List.of(cita(1L, veterinario)));

        assertEquals(1, citaService.listarCitasPorVeterinario(5L).size());
    }

    @Test
    @DisplayName("Un veterinario puede listar sus propias citas")
    void listarCitasPorVeterinario_veterinarioPropio_devuelveLasCitas() {
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(veterinario);
        when(citaRepository.findByVeterinarioId(5L)).thenReturn(List.of(cita(1L, veterinario)));

        assertEquals(1, citaService.listarCitasPorVeterinario(5L).size());
    }

    @Test
    @DisplayName("Un veterinario no puede listar las citas de otro veterinario")
    void listarCitasPorVeterinario_veterinarioAjeno_lanzaAccesoDenegado() {
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(veterinario);

        assertThrows(AccessDeniedException.class, () -> citaService.listarCitasPorVeterinario(6L));
        verify(citaRepository, never()).findByVeterinarioId(any());
    }

    @Test
    @DisplayName("El ADMIN puede listar las citas entre dos fechas")
    void listarCitasEntreFechas_admin_devuelveLasCitas() {
        LocalDateTime fin = fecha.plusDays(7);
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(admin);
        when(citaRepository.findByFechaHoraBetween(fecha, fin)).thenReturn(List.of(cita(1L, veterinario)));

        assertEquals(1, citaService.listarCitasEntreFechas(fecha, fin).size());
    }

    @Test
    @DisplayName("Solo el ADMIN puede listar las citas por fecha")
    void listarCitasEntreFechas_noAdmin_lanzaAccesoDenegado() {
        when(autorizacionService.obtenerUsuarioAutenticado()).thenReturn(veterinario);

        assertThrows(AccessDeniedException.class, () -> citaService.listarCitasEntreFechas(fecha, fecha.plusDays(1)));
        verify(citaRepository, never()).findByFechaHoraBetween(any(), any());
    }
}
