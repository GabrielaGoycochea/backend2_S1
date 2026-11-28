package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.exception.RecursoNoEncontradoException;
import com.petsalud.backendpetsalud.security.ActividadSospechosaService;
import com.petsalud.backendpetsalud.security.JwtRequestFilter;
import com.petsalud.backendpetsalud.security.JwtUtil;
import com.petsalud.backendpetsalud.security.SecurityConfig;
import com.petsalud.backendpetsalud.security.UsuarioDetailsService;
import com.petsalud.backendpetsalud.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Semana 5 (retroalimentación de la semana 4): capa web de UsuarioController con @WebMvcTest.
 * Se carga solo el controlador, la configuración de seguridad real y el manejador global de errores;
 * el servicio se simula con @MockBean (Spring crea un mock limpio para cada prueba).
 * Cada prueba sigue la estructura AAA: Arrange (stub del servicio), Act (mvc.perform) y Assert (código HTTP, JSON y verify).
 */
@org.junit.jupiter.api.Tag("integration")
@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, JwtRequestFilter.class, ActividadSospechosaService.class})
@DisplayName("UsuarioController: contrato HTTP con seguridad real")
class UsuarioControllerTest {

    private static final String REGISTRO_VALIDO = """
            {"email":"Ana.Rojas@PetSalud.cl","password":"Clave2026","nombre":"Ana","apellido":"Rojas",
             "telefono":"+56 9 1234 5678","direccion":"Av. Libertad 123"}""";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UsuarioService usuarioService;
    @MockBean
    private UsuarioDetailsService usuarioDetailsService;
    @MockBean
    private JwtUtil jwtUtil;

    private static Usuario ana() {
        return Usuario.builder().id(1L).email("ana.rojas@petsalud.cl").password("$2a$10$hashDePrueba")
                .nombre("Ana").apellido("Rojas").rol(Rol.CLIENTE).build();
    }

    // ---------------------------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /api/usuarios/registro (público)")
    class Registro {

        @Test
        @DisplayName("Datos válidos: 200, email normalizado y sin contraseña en la respuesta")
        void registro_datosValidos_responde200SinPassword() throws Exception {
            // Arrange
            when(usuarioService.registrarUsuario(any(Usuario.class))).thenReturn(ana());

            // Act
            ResultActions respuesta = mvc.perform(post("/api/usuarios/registro")
                    .contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO));

            // Assert
            respuesta.andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("ana.rojas@petsalud.cl"))
                    .andExpect(jsonPath("$.rol").value("CLIENTE"))
                    .andExpect(jsonPath("$.password").doesNotExist());
            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioService).registrarUsuario(captor.capture());
            assertEquals("ana.rojas@petsalud.cl", captor.getValue().getEmail());
        }

        @Test
        @DisplayName("Datos incompletos: 400 con los campos obligatorios y sin llamar al servicio")
        void registro_datosIncompletos_responde400() throws Exception {
            // Act
            ResultActions respuesta = mvc.perform(post("/api/usuarios/registro")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"));

            // Assert
            respuesta.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos", hasItems("email", "password", "nombre")));
            verifyNoInteractions(usuarioService);
        }

        @Test
        @DisplayName("Email con formato inválido y contraseña sin números: 400")
        void registro_emailYPasswordInvalidos_responde400() throws Exception {
            // Arrange
            String cuerpo = """
                    {"email":"ana.rojas","password":"SoloLetras","nombre":"Ana"}""";

            // Act
            ResultActions respuesta = mvc.perform(post("/api/usuarios/registro")
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpo));

            // Assert
            respuesta.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos", hasItems("email", "password")));
            verifyNoInteractions(usuarioService);
        }

        @Test
        @DisplayName("Email ya registrado: 400 con el mensaje del servicio")
        void registro_emailDuplicado_responde400() throws Exception {
            // Arrange
            when(usuarioService.registrarUsuario(any(Usuario.class)))
                    .thenThrow(new IllegalArgumentException("El email ya está registrado"));

            // Act
            ResultActions respuesta = mvc.perform(post("/api/usuarios/registro")
                    .contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO));

            // Assert
            respuesta.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("El email ya está registrado"));
        }
    }

    // ---------------------------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /api/usuarios y /api/usuarios/{id} (solo ADMIN)")
    class Consultas {

        @Test
        @WithMockUser(authorities = "ADMIN")
        @DisplayName("ADMIN consulta un usuario existente: 200 sin contraseña")
        void obtener_existente_responde200() throws Exception {
            // Arrange
            when(usuarioService.obtenerUsuarioPorId(1L)).thenReturn(Optional.of(ana()));

            // Act
            ResultActions respuesta = mvc.perform(get("/api/usuarios/1"));

            // Assert
            respuesta.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value("Ana"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @WithMockUser(authorities = "ADMIN")
        @DisplayName("ADMIN consulta un id inexistente: 404")
        void obtener_inexistente_responde404() throws Exception {
            // Arrange
            when(usuarioService.obtenerUsuarioPorId(99L)).thenReturn(Optional.empty());

            // Act
            ResultActions respuesta = mvc.perform(get("/api/usuarios/99"));

            // Assert
            respuesta.andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "ADMIN")
        @DisplayName("ADMIN lista los usuarios: 200 con la lista completa")
        void listar_admin_responde200() throws Exception {
            // Arrange
            when(usuarioService.listarUsuarios()).thenReturn(List.of(ana(), ana()));

            // Act
            ResultActions respuesta = mvc.perform(get("/api/usuarios"));

            // Assert
            respuesta.andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @WithMockUser(authorities = "CLIENTE")
        @DisplayName("CLIENTE intenta consultar un usuario: 403 y el servicio no se llama")
        void obtener_cliente_responde403() throws Exception {
            // Act
            ResultActions respuesta = mvc.perform(get("/api/usuarios/1"));

            // Assert
            respuesta.andExpect(status().isForbidden());
            verifyNoInteractions(usuarioService);
        }

        @Test
        @WithMockUser(authorities = "VETERINARIO")
        @DisplayName("VETERINARIO intenta listar usuarios: 403")
        void listar_veterinario_responde403() throws Exception {
            // Act
            ResultActions respuesta = mvc.perform(get("/api/usuarios"));

            // Assert
            respuesta.andExpect(status().isForbidden());
            verifyNoInteractions(usuarioService);
        }

        @Test
        @DisplayName("Sin token: 401")
        void listar_sinToken_responde401() throws Exception {
            // Act
            ResultActions respuesta = mvc.perform(get("/api/usuarios"));

            // Assert
            respuesta.andExpect(status().isUnauthorized());
            verifyNoInteractions(usuarioService);
        }
    }

    // ---------------------------------------------------------------------------------------------
    @Nested
    @DisplayName("PUT y DELETE /api/usuarios/{id} (solo ADMIN)")
    class ActualizacionYEliminacion {

        @Test
        @WithMockUser(authorities = "ADMIN")
        @DisplayName("ADMIN actualiza con datos válidos: 200")
        void actualizar_datosValidos_responde200() throws Exception {
            // Arrange
            Usuario actualizado = ana();
            actualizado.setNombre("Ana María");
            when(usuarioService.actualizarUsuario(eq(1L), any(Usuario.class))).thenReturn(actualizado);

            // Act
            ResultActions respuesta = mvc.perform(put("/api/usuarios/1").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nombre\":\"Ana María\",\"telefono\":\"+56 9 8765 4321\"}"));

            // Assert
            respuesta.andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Ana María"));
        }

        @Test
        @WithMockUser(authorities = "ADMIN")
        @DisplayName("ADMIN actualiza con nombre vacío: 400 y el servicio no se llama")
        void actualizar_nombreVacio_responde400() throws Exception {
            // Act
            ResultActions respuesta = mvc.perform(put("/api/usuarios/1")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"));

            // Assert
            respuesta.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos", hasItems("nombre")));
            verifyNoInteractions(usuarioService);
        }

        @Test
        @WithMockUser(authorities = "ADMIN")
        @DisplayName("ADMIN actualiza un id inexistente: 404, igual que al consultarlo")
        void actualizar_inexistente_responde404() throws Exception {
            // Arrange: el servicio real lanza esta excepción cuando el id no existe
            when(usuarioService.actualizarUsuario(eq(99L), any(Usuario.class)))
                    .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado"));

            // Act
            ResultActions respuesta = mvc.perform(put("/api/usuarios/99")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ana\"}"));

            // Assert
            respuesta.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
        }

        @Test
        @WithMockUser(authorities = "CLIENTE")
        @DisplayName("CLIENTE intenta actualizar: 403")
        void actualizar_cliente_responde403() throws Exception {
            // Act
            ResultActions respuesta = mvc.perform(put("/api/usuarios/1")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ana\"}"));

            // Assert
            respuesta.andExpect(status().isForbidden());
            verifyNoInteractions(usuarioService);
        }

        @Test
        @WithMockUser(authorities = "ADMIN")
        @DisplayName("ADMIN elimina un usuario: 204")
        void eliminar_admin_responde204() throws Exception {
            // Act
            ResultActions respuesta = mvc.perform(delete("/api/usuarios/1"));

            // Assert
            respuesta.andExpect(status().isNoContent());
            verify(usuarioService).eliminarUsuario(1L);
        }

        @Test
        @WithMockUser(authorities = "CLIENTE")
        @DisplayName("CLIENTE intenta eliminar: 403 y no se elimina nada")
        void eliminar_cliente_responde403() throws Exception {
            // Act
            ResultActions respuesta = mvc.perform(delete("/api/usuarios/1"));

            // Assert
            respuesta.andExpect(status().isForbidden());
            verifyNoInteractions(usuarioService);
        }
    }
}
