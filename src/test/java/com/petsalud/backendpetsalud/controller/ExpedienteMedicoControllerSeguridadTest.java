package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.security.ActividadSospechosaService;
import com.petsalud.backendpetsalud.security.JwtRequestFilter;
import com.petsalud.backendpetsalud.security.JwtUtil;
import com.petsalud.backendpetsalud.security.SecurityConfig;
import com.petsalud.backendpetsalud.security.UsuarioDetailsService;
import com.petsalud.backendpetsalud.service.ExpedienteMedicoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Semana 6: acceso a /api/expedientes con la seguridad real (autenticacion y roles).
 * Solo el personal (VETERINARIO o ADMIN) accede a los expedientes; eliminar es solo del ADMIN.
 */
@Tag("integration")
@WebMvcTest(ExpedienteMedicoController.class)
@Import({SecurityConfig.class, JwtRequestFilter.class, ActividadSospechosaService.class})
@DisplayName("ExpedienteMedicoController: acceso por autenticacion y rol")
class ExpedienteMedicoControllerSeguridadTest {

    @Autowired private MockMvc mvc;

    @MockBean private ExpedienteMedicoService expedienteMedicoService;
    @MockBean private UsuarioDetailsService usuarioDetailsService;
    @MockBean private JwtUtil jwtUtil;

    @Test
    @DisplayName("Sin token: 401 y el servicio no se llama")
    void listarPorMascota_sinToken_responde401() throws Exception {
        mvc.perform(get("/api/expedientes/mascota/1")).andExpect(status().isUnauthorized());

        verifyNoInteractions(expedienteMedicoService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    @DisplayName("Un CLIENTE no accede a los expedientes: 403")
    void listarPorMascota_cliente_responde403() throws Exception {
        mvc.perform(get("/api/expedientes/mascota/1")).andExpect(status().isForbidden());

        verifyNoInteractions(expedienteMedicoService);
    }

    @Test
    @WithMockUser(authorities = "VETERINARIO")
    @DisplayName("Un VETERINARIO accede a los expedientes de una mascota: 200")
    void listarPorMascota_veterinario_responde200() throws Exception {
        when(expedienteMedicoService.listarRegistrosPorMascota(1L)).thenReturn(List.of());

        mvc.perform(get("/api/expedientes/mascota/1")).andExpect(status().isOk());

        verify(expedienteMedicoService).listarRegistrosPorMascota(1L);
    }

    @Test
    @WithMockUser(authorities = "VETERINARIO")
    @DisplayName("Un registro incompleto rechazado por el servicio responde 400")
    void agregarRegistro_incompleto_responde400() throws Exception {
        when(expedienteMedicoService.agregarRegistro(anyLong(), anyLong(), any()))
                .thenThrow(new IllegalArgumentException("El registro medico esta incompleto"));

        mvc.perform(post("/api/expedientes").param("mascotaId", "1").param("veterinarioId", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"Control anual\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "VETERINARIO")
    @DisplayName("Un VETERINARIO no puede eliminar registros: 403")
    void eliminarRegistro_veterinario_responde403() throws Exception {
        mvc.perform(delete("/api/expedientes/3")).andExpect(status().isForbidden());

        verifyNoInteractions(expedienteMedicoService);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("Un ADMIN puede eliminar registros: 204")
    void eliminarRegistro_admin_responde204() throws Exception {
        mvc.perform(delete("/api/expedientes/3")).andExpect(status().isNoContent());

        verify(expedienteMedicoService).eliminarRegistro(3L);
    }
}
