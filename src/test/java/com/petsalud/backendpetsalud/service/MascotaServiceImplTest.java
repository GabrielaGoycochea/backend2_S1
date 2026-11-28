package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * S5: pruebas unitarias del servicio de mascotas y de su asociacion con el propietario.
 * Los repositorios y la autorizacion se simulan con Mockito: no se usa MySQL.
 */
@ExtendWith(MockitoExtension.class)
class MascotaServiceImplTest {

    @Mock private MascotaRepository mascotaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AutorizacionService autorizacionService;

    @InjectMocks
    private MascotaServiceImpl mascotaService;

    private Usuario cliente;
    private Mascota mascota;

    @BeforeEach
    void prepararDatos() {
        cliente = Usuario.builder().id(3L).email("cliente@petsalud.cl").nombre("Ana").rol(Rol.CLIENTE).build();
        mascota = Mascota.builder().id(1L).nombre("Firulais").especie("Perro").propietario(cliente).build();
    }

    @Test
    @DisplayName("Registrar una mascota la asocia a su propietario")
    void registrarMascota_propietarioExistente_asociaPropietario() {
        Mascota nueva = Mascota.builder().nombre("Michi").especie("Gato").build();
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(cliente));
        when(mascotaRepository.save(any(Mascota.class))).thenAnswer(i -> i.getArgument(0));

        Mascota resultado = mascotaService.registrarMascota(nueva, 3L);

        assertSame(cliente, resultado.getPropietario());
        verify(autorizacionService).verificarPersonalOPropietario(3L);
        verify(mascotaRepository).save(nueva);
    }

    @Test
    @DisplayName("No se registra una mascota para un propietario inexistente")
    void registrarMascota_propietarioInexistente_lanzaError() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> mascotaService.registrarMascota(new Mascota(), 99L));

        verify(mascotaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Consultar una mascota existente verifica el acceso del usuario")
    void obtenerMascotaPorId_existe_verificaAcceso() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));

        assertTrue(mascotaService.obtenerMascotaPorId(1L).isPresent());
        verify(autorizacionService).verificarPersonalOPropietario(3L);
    }

    @Test
    @DisplayName("Consultar una mascota inexistente devuelve vacio sin verificar acceso")
    void obtenerMascotaPorId_noExiste_devuelveVacio() {
        when(mascotaRepository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(mascotaService.obtenerMascotaPorId(99L).isEmpty());
        verifyNoInteractions(autorizacionService);
    }

    @Test
    @DisplayName("Consultar una mascota sin propietario registrado lanza un error claro y no un NullPointerException")
    void obtenerMascotaPorId_sinPropietario_lanzaError() {
        mascota.setPropietario(null);
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> mascotaService.obtenerMascotaPorId(1L));

        assertTrue(error.getMessage().contains("propietario"));
        verifyNoInteractions(autorizacionService);
    }

    @Test
    @DisplayName("Actualizar una mascota cambia sus datos")
    void actualizarMascota_existente_actualizaLosDatos() {
        Mascota cambios = Mascota.builder().nombre("Rocky").especie("Perro").raza("Labrador").sexo("Macho").build();
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(mascotaRepository.save(any(Mascota.class))).thenAnswer(i -> i.getArgument(0));

        Mascota resultado = mascotaService.actualizarMascota(1L, cambios);

        assertEquals("Rocky", resultado.getNombre());
        assertEquals("Labrador", resultado.getRaza());
        assertEquals("Macho", resultado.getSexo());
        assertSame(cliente, resultado.getPropietario());
        verify(autorizacionService).verificarPropietarioOAdmin(3L);
    }

    @Test
    @DisplayName("No se actualiza una mascota inexistente")
    void actualizarMascota_inexistente_lanzaError() {
        when(mascotaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> mascotaService.actualizarMascota(99L, new Mascota()));

        verify(mascotaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Eliminar una mascota existente la borra del repositorio")
    void eliminarMascota_existente_laElimina() {
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));

        mascotaService.eliminarMascota(1L);

        verify(autorizacionService).verificarPropietarioOAdmin(3L);
        verify(mascotaRepository).delete(mascota);
    }

    @Test
    @DisplayName("No se elimina una mascota inexistente")
    void eliminarMascota_inexistente_lanzaError() {
        when(mascotaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> mascotaService.eliminarMascota(99L));

        verify(mascotaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Listar las mascotas de un propietario verifica el acceso y devuelve las del repositorio")
    void listarMascotasPorPropietario_propietario_verificaAccesoYDevuelveSusMascotas() {
        Mascota segunda = Mascota.builder().id(2L).nombre("Michi").especie("Gato").propietario(cliente).build();
        when(mascotaRepository.findByPropietarioId(3L)).thenReturn(List.of(mascota, segunda));

        List<Mascota> resultado = mascotaService.listarMascotasPorPropietario(3L);

        assertEquals(2, resultado.size());
        verify(autorizacionService).verificarPersonalOPropietario(3L);
    }

    @Test
    @DisplayName("Una mascota con propietario sin ID genera un error controlado")
    void obtenerMascotaPorId_propietarioSinId_lanzaError() {
        mascota.setPropietario(new Usuario());
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> mascotaService.obtenerMascotaPorId(1L));
        assertTrue(error.getMessage().contains("propietario"));
        verifyNoInteractions(autorizacionService);
    }
}
