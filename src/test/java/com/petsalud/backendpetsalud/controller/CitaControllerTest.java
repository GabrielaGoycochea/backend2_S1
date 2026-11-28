package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.EstadoCita;
import com.petsalud.backendpetsalud.security.*;
import com.petsalud.backendpetsalud.service.CitaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@WebMvcTest(CitaController.class)
@Import({SecurityConfig.class, JwtRequestFilter.class, ActividadSospechosaService.class})
class CitaControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private CitaService citaService;
    @MockBean private UsuarioDetailsService usuarioDetailsService;
    @MockBean private JwtUtil jwtUtil;

    private static final String API = "/api/citas";
    private static final LocalDateTime FECHA = LocalDateTime.of(2030,1,15,10,0);

    private Cita cita() {
        return Cita.builder().id(1L).fechaHora(FECHA).estado(EstadoCita.AGENDADA).motivo("Control").build();
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void agendarCita_responde200() throws Exception {
        when(citaService.agendarCita(1L,2L,FECHA,"Control")).thenReturn(cita());
        mvc.perform(post(API).param("mascotaId","1").param("veterinarioId","2").param("fechaHora",FECHA.toString()).param("motivo","Control"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Agendada")).andExpect(jsonPath("$.motivo").value("Control"));
        verify(citaService).agendarCita(1L,2L,FECHA,"Control");
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void consultarCitaExistente_responde200() throws Exception {
        when(citaService.obtenerCitaPorId(1L)).thenReturn(Optional.of(cita()));
        mvc.perform(get(API + "/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void consultarCitaInexistente_responde404() throws Exception {
        when(citaService.obtenerCitaPorId(99L)).thenReturn(Optional.empty());
        mvc.perform(get(API + "/99")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "VETERINARIO")
    void actualizarEstado_responde200() throws Exception {
        Cita actualizada = cita();
        actualizada.setEstado(EstadoCita.COMPLETADA);
        when(citaService.actualizarEstadoCita(1L,"Completada")).thenReturn(actualizada);
        mvc.perform(put(API + "/1/estado").param("estado","Completada")).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Completada"));
        verify(citaService).actualizarEstadoCita(1L,"Completada");
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void estadoInvalido_responde400() throws Exception {
        when(citaService.actualizarEstadoCita(1L,"Inventado")).thenThrow(new IllegalArgumentException("Estado de cita invalido"));
        mvc.perform(put(API + "/1/estado").param("estado","Inventado")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void clienteNoPuedeActualizarEstado() throws Exception {
        mvc.perform(put(API + "/1/estado").param("estado","Completada")).andExpect(status().isForbidden());
        verifyNoInteractions(citaService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void cancelarCita_responde204() throws Exception {
        mvc.perform(delete(API + "/1")).andExpect(status().isNoContent());
        verify(citaService).cancelarCita(1L);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void listarCitasPorMascota_responde200() throws Exception {
        when(citaService.listarCitasPorMascota(1L)).thenReturn(List.of(cita()));
        mvc.perform(get(API + "/mascota/1")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser(authorities = "VETERINARIO")
    void listarCitasPorVeterinario_responde200() throws Exception {
        when(citaService.listarCitasPorVeterinario(2L)).thenReturn(List.of(cita()));
        mvc.perform(get(API + "/veterinario/2")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void listarCitasEntreFechas_responde200() throws Exception {
        when(citaService.listarCitasEntreFechas(FECHA,FECHA.plusDays(1))).thenReturn(List.of(cita()));
        mvc.perform(get(API + "/entre-fechas").param("inicio",FECHA.toString()).param("fin",FECHA.plusDays(1).toString())).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void clienteNoPuedeListarCitasPorVeterinario() throws Exception {
        mvc.perform(get(API + "/veterinario/2")).andExpect(status().isForbidden());
        verifyNoInteractions(citaService);
    }
}
