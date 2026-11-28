package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.security.*;
import com.petsalud.backendpetsalud.service.MascotaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@WebMvcTest(MascotaController.class)
@Import({SecurityConfig.class, JwtRequestFilter.class, ActividadSospechosaService.class})
class MascotaControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private MascotaService mascotaService;
    @MockBean private UsuarioDetailsService usuarioDetailsService;
    @MockBean private JwtUtil jwtUtil;

    private static final String API = "/api/mascotas";
    private static final String DATOS = "{\"nombre\":\"Luna\",\"especie\":\"Gato\"}";

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void registrarMascota_responde200() throws Exception {
        when(mascotaService.registrarMascota(any(Mascota.class), eq(3L))).thenAnswer(i -> i.getArgument(0));
        mvc.perform(post(API + "/propietario/3").contentType(MediaType.APPLICATION_JSON).content(DATOS))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Luna")).andExpect(jsonPath("$.especie").value("Gato"));
        verify(mascotaService).registrarMascota(any(Mascota.class), eq(3L));
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void consultarMascotaExistente_responde200() throws Exception {
        when(mascotaService.obtenerMascotaPorId(1L)).thenReturn(Optional.of(Mascota.builder().id(1L).nombre("Luna").build()));
        mvc.perform(get(API + "/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.nombre").value("Luna"));
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void consultarMascotaInexistente_responde404() throws Exception {
        when(mascotaService.obtenerMascotaPorId(99L)).thenReturn(Optional.empty());
        mvc.perform(get(API + "/99")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void actualizarMascota_responde200() throws Exception {
        when(mascotaService.actualizarMascota(eq(1L), any(Mascota.class))).thenAnswer(i -> i.getArgument(1));
        mvc.perform(put(API + "/1").contentType(MediaType.APPLICATION_JSON).content(DATOS)).andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Luna"));
        verify(mascotaService).actualizarMascota(eq(1L), any(Mascota.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void eliminarMascota_responde204() throws Exception {
        mvc.perform(delete(API + "/1")).andExpect(status().isNoContent());
        verify(mascotaService).eliminarMascota(1L);
    }

    @Test
    @WithMockUser(authorities = "VETERINARIO")
    void listarMascotasPorPropietario_responde200() throws Exception {
        when(mascotaService.listarMascotasPorPropietario(3L)).thenReturn(List.of(Mascota.builder().id(1L).nombre("Luna").build()));
        mvc.perform(get(API + "/propietario/3")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].nombre").value("Luna"));
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void consultarMascotaAjena_responde403() throws Exception {
        when(mascotaService.obtenerMascotaPorId(1L)).thenThrow(new AccessDeniedException("Acceso denegado"));
        mvc.perform(get(API + "/1")).andExpect(status().isForbidden());
    }
}
