package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.entity.ExpedienteMedico;
import com.petsalud.backendpetsalud.security.*;
import com.petsalud.backendpetsalud.service.ExpedienteMedicoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@WebMvcTest(ExpedienteMedicoController.class)
@Import({SecurityConfig.class, JwtRequestFilter.class, ActividadSospechosaService.class})
class ExpedienteMedicoControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private ExpedienteMedicoService expedienteMedicoService;
    @MockBean private UsuarioDetailsService usuarioDetailsService;
    @MockBean private JwtUtil jwtUtil;

    private static final String API = "/api/expedientes";
    private static final String DATOS = "{\"descripcion\":\"Control\",\"diagnostico\":\"Salud estable\",\"tratamiento\":\"Seguimiento\"}";

    @Test
    @WithMockUser(authorities = "VETERINARIO")
    void registroValido_responde200() throws Exception {
        when(expedienteMedicoService.agregarRegistro(eq(1L), eq(2L), any(ExpedienteMedico.class))).thenAnswer(i -> i.getArgument(2));
        mvc.perform(post(API).param("mascotaId","1").param("veterinarioId","2").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isOk()).andExpect(jsonPath("$.descripcion").value("Control"))
                .andExpect(jsonPath("$.diagnostico").value("Salud estable")).andExpect(jsonPath("$.tratamiento").value("Seguimiento"));
        verify(expedienteMedicoService).agregarRegistro(eq(1L), eq(2L), any(ExpedienteMedico.class));
    }

    @Test
    @WithMockUser(authorities = "VETERINARIO")
    void descripcionVacia_responde400() throws Exception {
        mvc.perform(post(API).param("mascotaId","1").param("veterinarioId","2").contentType(MediaType.APPLICATION_JSON).content("{\"descripcion\":\"\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(expedienteMedicoService);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void consultarRegistroExistente_responde200() throws Exception {
        when(expedienteMedicoService.obtenerRegistroPorId(1L)).thenReturn(Optional.of(ExpedienteMedico.builder().id(1L).descripcion("Control").build()));
        mvc.perform(get(API + "/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.descripcion").value("Control"));
    }

    @Test
    @WithMockUser(authorities = "VETERINARIO")
    void consultarRegistroInexistente_responde404() throws Exception {
        when(expedienteMedicoService.obtenerRegistroPorId(99L)).thenReturn(Optional.empty());
        mvc.perform(get(API + "/99")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "VETERINARIO")
    void listarExpedientesPorMascota_responde200() throws Exception {
        when(expedienteMedicoService.listarRegistrosPorMascota(1L)).thenReturn(List.of(ExpedienteMedico.builder().id(10L).build()));
        mvc.perform(get(API + "/mascota/1")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void clienteNoPuedeConsultarExpedientes() throws Exception {
        mvc.perform(get(API + "/1")).andExpect(status().isForbidden());
        mvc.perform(get(API + "/mascota/1")).andExpect(status().isForbidden());
        verifyNoInteractions(expedienteMedicoService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void clienteNoPuedeRegistrarExpedientes() throws Exception {
        mvc.perform(post(API).param("mascotaId","1").param("veterinarioId","2").contentType(MediaType.APPLICATION_JSON).content(DATOS)).andExpect(status().isForbidden());
        verifyNoInteractions(expedienteMedicoService);
    }

    @Test
    void solicitudSinAutenticacion_responde401() throws Exception {
        mvc.perform(get(API + "/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(expedienteMedicoService);
    }
}
