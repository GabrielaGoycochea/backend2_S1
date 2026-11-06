package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.controller.ExpedienteMedicoController;
import com.petsalud.backendpetsalud.controller.NotificacionController;
import com.petsalud.backendpetsalud.controller.RecompensaController;
import com.petsalud.backendpetsalud.entity.Notificacion;
import com.petsalud.backendpetsalud.service.ExpedienteMedicoService;
import com.petsalud.backendpetsalud.service.NotificacionService;
import com.petsalud.backendpetsalud.service.RecompensaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterio 4: cada endpoint declara su regla de acceso con @PreAuthorize.
 * Los casos HTTP usan endpoints donde la regla por URL solo exige "autenticado",
 * para demostrar que es la anotacion del metodo la que bloquea al rol incorrecto
 * antes de llegar al servicio.
 */
@WebMvcTest(controllers = {NotificacionController.class, RecompensaController.class, ExpedienteMedicoController.class})
@Import({SecurityConfig.class, JwtRequestFilter.class, ActividadSospechosaService.class})
class PreAuthorizeTest {

    private static final String NOTIFICACION = "{\"mensaje\":\"Campana de vacunacion\",\"tipo\":\"Promocion\"}";

    @Autowired private MockMvc mvc;
    @MockBean private NotificacionService notificaciones;
    @MockBean private RecompensaService recompensas;
    @MockBean private ExpedienteMedicoService expedientes;
    @MockBean private UsuarioDetailsService usuarios;
    @MockBean private JwtUtil jwtUtil;

    @Test
    void todosLosEndpointsDeclaranPreAuthorize() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> sinRegla = new ArrayList<>();
        int endpoints = 0;
        for (BeanDefinition bd : scanner.findCandidateComponents("com.petsalud.backendpetsalud.controller")) {
            for (Method metodo : Class.forName(bd.getBeanClassName()).getDeclaredMethods()) {
                if (AnnotatedElementUtils.hasAnnotation(metodo, RequestMapping.class)) {
                    endpoints++;
                    if (!AnnotatedElementUtils.hasAnnotation(metodo, PreAuthorize.class)) {
                        sinRegla.add(metodo.getDeclaringClass().getSimpleName() + "." + metodo.getName());
                    }
                }
            }
        }
        assertEquals(43, endpoints, "cantidad de endpoints revisados");
        assertTrue(sinRegla.isEmpty(), "Endpoints sin @PreAuthorize: " + sinRegla);
    }

    @Test
    void sinTokenNoPuedeEnviarNotificaciones() throws Exception {
        mvc.perform(post("/api/notificaciones/usuario/1").contentType(MediaType.APPLICATION_JSON).content(NOTIFICACION))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(notificaciones);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void clienteNoPuedeEnviarNotificaciones() throws Exception {
        mvc.perform(post("/api/notificaciones/usuario/1").contentType(MediaType.APPLICATION_JSON).content(NOTIFICACION))
                .andExpect(status().isForbidden());
        verifyNoInteractions(notificaciones);
    }

    @Test
    @WithMockUser(authorities = "VETERINARIO")
    void empleadoNoPuedeEnviarNotificaciones() throws Exception {
        mvc.perform(post("/api/notificaciones/usuario/1").contentType(MediaType.APPLICATION_JSON).content(NOTIFICACION))
                .andExpect(status().isForbidden());
        verifyNoInteractions(notificaciones);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void gerentePuedeEnviarNotificaciones() throws Exception {
        when(notificaciones.enviarNotificacion(eq(1L), any(Notificacion.class))).thenReturn(new Notificacion());
        mvc.perform(post("/api/notificaciones/usuario/1").contentType(MediaType.APPLICATION_JSON).content(NOTIFICACION))
                .andExpect(status().isOk());
        verify(notificaciones).enviarNotificacion(eq(1L), any(Notificacion.class));
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void clienteNoPuedeEliminarRecompensas() throws Exception {
        mvc.perform(delete("/api/recompensas/1")).andExpect(status().isForbidden());
        verifyNoInteractions(recompensas);
    }

    @Test
    @WithMockUser(authorities = "VETERINARIO")
    void empleadoNoPuedeEliminarExpedientes() throws Exception {
        mvc.perform(delete("/api/expedientes/1")).andExpect(status().isForbidden());
        verifyNoInteractions(expedientes);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void gerentePuedeEliminarExpedientes() throws Exception {
        mvc.perform(delete("/api/expedientes/1")).andExpect(status().isNoContent());
        verify(expedientes).eliminarRegistro(1L);
    }
}
