package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.ExpedienteMedico;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.ExpedienteMedicoRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Semana 6 (TDD, ciclo 4): registros medicos completos y firmados por un veterinario.
 * Repositorios y autorizacion simulados con Mockito (sin MySQL).
 */
@ExtendWith(MockitoExtension.class)
class ExpedienteMedicoServiceImplTest {

    @Mock private ExpedienteMedicoRepository expedienteMedicoRepository;
    @Mock private MascotaRepository mascotaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AutorizacionService autorizacionService;

    @InjectMocks
    private ExpedienteMedicoServiceImpl expedienteService;

    private Mascota mascota;
    private Usuario veterinario;

    @BeforeEach
    void prepararDatos() {
        mascota = Mascota.builder().id(1L).nombre("Firulais").especie("Perro").build();
        veterinario = Usuario.builder().id(5L).email("vet@petsalud.cl").nombre("Luis").rol(Rol.VETERINARIO).build();
    }

    private ExpedienteMedico registroCompleto() {
        return ExpedienteMedico.builder()
                .descripcion("Control anual").diagnostico("Sano").tratamiento("Vacuna anual").build();
    }

    @Test
    @DisplayName("Un registro completo queda vinculado a la mascota y al veterinario, con fecha")
    void agregarRegistro_registroCompleto_quedaVinculadoYConFecha() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(veterinario));
        when(expedienteMedicoRepository.save(any(ExpedienteMedico.class))).thenAnswer(i -> i.getArgument(0));

        ExpedienteMedico resultado = expedienteService.agregarRegistro(1L, 5L, registroCompleto());

        assertSame(mascota, resultado.getMascota());
        assertSame(veterinario, resultado.getVeterinario());
        assertNotNull(resultado.getFecha());
        assertEquals("Sano", resultado.getDiagnostico());
        verify(autorizacionService).verificarIdentidadVeterinario(5L);
    }

    @ParameterizedTest
    @CsvSource({"descripcion", "diagnostico", "tratamiento"})
    @DisplayName("Un registro al que le falta un dato clinico no se guarda")
    void agregarRegistro_faltaUnDato_lanzaError(String campo) {
        ExpedienteMedico registro = registroCompleto();
        switch (campo) {
            case "descripcion" -> registro.setDescripcion(null);
            case "diagnostico" -> registro.setDiagnostico("   ");
            default -> registro.setTratamiento("");
        }

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> expedienteService.agregarRegistro(1L, 5L, registro));

        assertTrue(error.getMessage().contains(campo));
        verify(expedienteMedicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un registro nulo se rechaza")
    void agregarRegistro_registroNulo_lanzaError() {
        assertThrows(IllegalArgumentException.class, () -> expedienteService.agregarRegistro(1L, 5L, null));

        verify(expedienteMedicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("No se registra en una mascota inexistente")
    void agregarRegistro_mascotaInexistente_lanzaError() {
        when(mascotaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> expedienteService.agregarRegistro(9L, 5L, registroCompleto()));

        verify(expedienteMedicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("No se registra con un veterinario inexistente")
    void agregarRegistro_veterinarioInexistente_lanzaError() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> expedienteService.agregarRegistro(1L, 9L, registroCompleto()));

        verify(expedienteMedicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("El usuario que firma el registro debe tener rol VETERINARIO")
    void agregarRegistro_usuarioQueNoEsVeterinario_lanzaError() {
        veterinario.setRol(Rol.CLIENTE);
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(veterinario));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> expedienteService.agregarRegistro(1L, 5L, registroCompleto()));

        assertTrue(error.getMessage().contains("veterinario"));
        verify(expedienteMedicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Consultar, listar y eliminar delegan en el repositorio")
    void consultarListarYEliminar_delegaEnElRepositorio() {
        ExpedienteMedico registro = registroCompleto();
        when(expedienteMedicoRepository.findById(3L)).thenReturn(Optional.of(registro));
        when(expedienteMedicoRepository.findByMascotaId(1L)).thenReturn(List.of(registro));

        assertTrue(expedienteService.obtenerRegistroPorId(3L).isPresent());
        assertEquals(1, expedienteService.listarRegistrosPorMascota(1L).size());
        expedienteService.eliminarRegistro(3L);

        verify(expedienteMedicoRepository).deleteById(3L);
    }
}
