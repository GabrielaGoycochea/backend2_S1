package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.controller.AuthController;
import com.petsalud.backendpetsalud.controller.CitaController;
import com.petsalud.backendpetsalud.controller.ExpedienteMedicoController;
import com.petsalud.backendpetsalud.controller.MascotaController;
import com.petsalud.backendpetsalud.controller.ProductoController;
import com.petsalud.backendpetsalud.controller.UsuarioController;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.service.CitaService;
import com.petsalud.backendpetsalud.service.ExpedienteMedicoService;
import com.petsalud.backendpetsalud.service.MascotaService;
import com.petsalud.backendpetsalud.service.ProductoService;
import com.petsalud.backendpetsalud.service.UsuarioService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterio 5: pruebas de proteccion contra SQL Injection, XSS y CSRF sobre la API real
 * (controladores, validacion y cadena de Spring Security), con los servicios simulados.
 * Cada caso comprueba que el ataque se rechaza ANTES de llegar al servicio.
 */
@ExtendWith(OutputCaptureExtension.class)
@WebMvcTest(controllers = {AuthController.class, UsuarioController.class, MascotaController.class,
        ExpedienteMedicoController.class, CitaController.class, ProductoController.class})
@Import({SecurityConfig.class, JwtRequestFilter.class, ActividadSospechosaService.class, JwtUtil.class, LoginAttemptService.class})
@TestPropertySource(properties = {"jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "jwt.expiration-ms=1800000",
        "jwt.issuer=PetSalud", "cors.allowed-origins=http://localhost:3000"})
class AmenazasComunesTest {

    private static final String SQLI = "' OR '1'='1' --";

    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder encoder;
    @MockBean private UsuarioDetailsService usuarios;
    @MockBean private UsuarioService usuarioService;
    @MockBean private MascotaService mascotaService;
    @MockBean private ExpedienteMedicoService expedienteService;
    @MockBean private CitaService citaService;
    @MockBean private ProductoService productoService;

    @Nested
    class InyeccionSql {

        @Test
        void loginConInyeccionSeTrataComoTextoYResponde401() throws Exception {
            when(usuarios.loadUserByUsername(anyString())).thenThrow(new UsernameNotFoundException("no existe"));

            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + SQLI + "\",\"password\":\"" + SQLI + "\"}"))
                    .andExpect(status().isUnauthorized());

            // El texto completo llega como un solo valor (parametro), no como parte de la consulta
            verify(usuarios).loadUserByUsername("' or '1'='1' --");
        }

        @Test
        void registroConInyeccionEnElEmailSeRechazaPorValidacion() throws Exception {
            mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"cliente@petsalud.cl' OR '1'='1\",\"password\":\"Clave1234\",\"nombre\":\"Ana\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos[0]").value("email"));
            verifyNoInteractions(usuarioService);
        }

        @Test
        void busquedaPorCategoriaConInyeccionSeRechaza() throws Exception {
            mvc.perform(get("/api/productos/categoria").param("categoria", "Alimentos" + SQLI))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos[0]").value("categoria"));
            verifyNoInteractions(productoService);
        }

        @Test
        @WithMockUser(username = "cliente@petsalud.cl", authorities = "CLIENTE")
        void idDeRutaConInyeccionSeRechaza() throws Exception {
            mvc.perform(get("/api/mascotas/{id}", "1 OR 1=1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos[0]").value("id"));
            verifyNoInteractions(mascotaService);
        }
    }

    @Nested
    class CrossSiteScripting {

        @Test
        @WithMockUser(username = "cliente@petsalud.cl", authorities = "CLIENTE")
        void scriptEnElNombreDeLaMascotaSeRechazaYQuedaRegistrado(CapturedOutput salida) throws Exception {
            mvc.perform(post("/api/mascotas/propietario/3").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"<script>alert('xss')</script>\",\"especie\":\"Perro\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos[0]").value("nombre"));
            verifyNoInteractions(mascotaService);
            assertThat(salida.getOut())
                    .contains("ENTRADA_RECHAZADA usuario=cliente@petsalud.cl ip=127.0.0.1 POST /api/mascotas/propietario/3 campos=nombre")
                    .doesNotContain("<script>");
        }

        @Test
        @WithMockUser(username = "vet@petsalud.cl", authorities = "VETERINARIO")
        void htmlEnElDiagnosticoSeRechaza() throws Exception {
            mvc.perform(post("/api/expedientes").param("mascotaId", "1").param("veterinarioId", "2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"descripcion\":\"Control anual\",\"diagnostico\":\"<img src=x onerror=alert(1)>\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos[0]").value("diagnostico"));
            verifyNoInteractions(expedienteService);
        }

        @Test
        @WithMockUser(username = "cliente@petsalud.cl", authorities = "CLIENTE")
        void scriptEnElMotivoDeLaCitaSeRechaza() throws Exception {
            mvc.perform(post("/api/citas").param("mascotaId", "1").param("veterinarioId", "2")
                            .param("fechaHora", "2026-12-01T10:00:00").param("motivo", "<script>alert(1)</script>"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos[0]").value("motivo"));
            verifyNoInteractions(citaService);
        }

        @Test
        @WithMockUser(username = "cliente@petsalud.cl", authorities = "CLIENTE")
        void datosValidosSeAceptanSinIdNiPropietarioDelCuerpo() throws Exception {
            when(mascotaService.registrarMascota(org.mockito.ArgumentMatchers.any(Mascota.class), eq(3L)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // El cuerpo intenta fijar el id de otra mascota y otro propietario (asignacion masiva)
            mvc.perform(post("/api/mascotas/propietario/3").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"id\":99,\"propietario\":{\"id\":1},\"nombre\":\"Luna\",\"especie\":\"Perro\","
                                    + "\"raza\":\"Mestiza\",\"sexo\":\"Hembra\",\"fechaNacimiento\":\"2021-05-10\"}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Mascota> capturada = ArgumentCaptor.forClass(Mascota.class);
            verify(mascotaService).registrarMascota(capturada.capture(), eq(3L));
            assertThat(capturada.getValue().getNombre()).isEqualTo("Luna");
            assertNull(capturada.getValue().getId());
            assertNull(capturada.getValue().getPropietario());
        }

        @Test
        void lasRespuestasIncluyenCabecerasContraXss() throws Exception {
            when(productoService.listarProductos()).thenReturn(List.of());
            mvc.perform(get("/api/productos"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"));
        }
    }

    @Nested
    class Csrf {

        private static final String PRODUCTO =
                "{\"nombre\":\"Alimento perro 15 kg\",\"precio\":24990,\"stock\":20,\"categoria\":\"Alimentos\"}";

        @Test
        void unaCookieNoAutentica() throws Exception {
            // Simula el ataque CSRF clasico: el navegador adjunta una cookie de sesion, pero sin el header Bearer
            mvc.perform(post("/api/productos").cookie(new Cookie("JSESSIONID", "sesion-robada"))
                            .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(productoService);
        }

        @Test
        void elLoginNoCreaSesionNiCookies() throws Exception {
            String hash = encoder.encode("Clave1234");
            when(usuarios.loadUserByUsername("csrf@petsalud.cl")).thenAnswer(inv ->
                    User.withUsername("csrf@petsalud.cl").password(hash).authorities("CLIENTE").build());

            MvcResult resultado = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"csrf@petsalud.cl\",\"password\":\"Clave1234\"}"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("Set-Cookie"))
                    .andReturn();
            assertNull(resultado.getRequest().getSession(false));
        }

        @Test
        void otroSitioNoPuedeLlamarALaApiDesdeElNavegador() throws Exception {
            mvc.perform(options("/api/productos")
                            .header("Origin", "https://sitio-malicioso.example")
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void elFrontAutorizadoSiPuedeYSinCredencialesAutomaticas() throws Exception {
            mvc.perform(options("/api/productos")
                            .header("Origin", "http://localhost:3000")
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        }
    }
}
