package com.petsalud.backendpetsalud;

import com.petsalud.backendpetsalud.entity.*;
import com.petsalud.backendpetsalud.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Matriz de pruebas de seguridad: cada perfil hace lo que debe hacer y no hace lo que no debe.
 * Se ejecuta con: mvnw test  (usa H2 en memoria, no necesita MySQL).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadIntegrationTest {

    private static final String PASS = "Petsalud2026!";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired MascotaRepository mascotas;
    @Autowired CitaRepository citas;
    @Autowired ExpedienteMedicoRepository expedientes;
    @Autowired VentaRepository ventas;
    @Autowired ProductoRepository productos;
    @Autowired NotificacionRepository notificaciones;
    @Autowired RecompensaRepository recompensas;
    @Autowired PasswordEncoder encoder;

    Usuario admin, vet, camila, tomas;
    Mascota mascotaCamila, mascotaTomas;

    @BeforeEach
    void datos() {
        citas.deleteAll();
        expedientes.deleteAll();
        ventas.deleteAll();
        notificaciones.deleteAll();
        recompensas.deleteAll();
        mascotas.deleteAll();
        productos.deleteAll();
        usuarios.deleteAll();

        admin = crear("admin@petsalud.cl", Rol.ADMIN);
        vet = crear("vet@petsalud.cl", Rol.VETERINARIO);
        camila = crear("camila@mail.cl", Rol.CLIENTE);
        tomas = crear("tomas@mail.cl", Rol.CLIENTE);
        mascotaCamila = mascotas.save(Mascota.builder().nombre("Luna").especie("Perro").propietario(camila).build());
        mascotaTomas = mascotas.save(Mascota.builder().nombre("Michi").especie("Gato").propietario(tomas).build());
        productos.save(Producto.builder().nombre("Alimento 15kg").precio(new BigDecimal("24990")).stock(10).categoria("Alimentos").build());
    }

    private Usuario crear(String email, Rol rol) {
        return usuarios.save(Usuario.builder().email(email).password(encoder.encode(PASS)).nombre("Test").rol(rol).build());
    }

    // ------------------------------------------------------------ Autenticacion

    @Test
    @DisplayName("Sin credenciales -> 401")
    void sinCredenciales() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Contrasena incorrecta -> 401")
    void contrasenaIncorrecta() throws Exception {
        mvc.perform(get("/api/auth/me").with(httpBasic("camila@mail.cl", "Otra1234")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Credenciales correctas -> 200 y entrega el rol")
    void loginCorrecto() throws Exception {
        mvc.perform(get("/api/auth/me").with(httpBasic("camila@mail.cl", PASS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    @DisplayName("Registro: rol forzado a CLIENTE, password con BCrypt y fuera del JSON")
    void registroSeguro() throws Exception {
        String json = """
                {"email":"prueba@correo.cl","password":"Clave1234","nombre":"Prueba","rol":"ADMIN"}
                """;
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist());
        Usuario guardado = usuarios.findByEmail("prueba@correo.cl").orElseThrow();
        assertThat(guardado.getPassword()).startsWith("$2a$");
    }

    @Test
    @DisplayName("Registro con email invalido, clave debil o script -> 400 con detalle por campo")
    void registroInvalido() throws Exception {
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-email\",\"password\":\"123\",\"nombre\":\"<script>\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.password").exists())
                .andExpect(jsonPath("$.campos.nombre").exists());
    }

    @Test
    @DisplayName("Registro con email repetido -> 400 (no 500)")
    void registroDuplicado() throws Exception {
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"camila@mail.cl\",\"password\":\"Clave1234\",\"nombre\":\"Camila\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Fuerza bruta: tras 5 intentos fallidos la cuenta se bloquea aunque luego use la clave correcta")
    void bloqueoPorIntentos() throws Exception {
        crear("victima@mail.cl", Rol.CLIENTE);
        for (int i = 0; i < 5; i++) {
            mvc.perform(get("/api/auth/me").with(httpBasic("victima@mail.cl", "Incorrecta" + i)))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/auth/me").with(httpBasic("victima@mail.cl", PASS)))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------ Autorizacion por rol

    @Test
    @DisplayName("Listar usuarios: CLIENTE 403, VETERINARIO 403, ADMIN 200 sin hashes")
    void listarUsuariosSoloAdmin() throws Exception {
        mvc.perform(get("/api/usuarios").with(httpBasic("camila@mail.cl", PASS))).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios").with(httpBasic("vet@petsalud.cl", PASS))).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios").with(httpBasic("admin@petsalud.cl", PASS)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    @DisplayName("Catalogo publico; crear producto solo ADMIN; stock bajo solo personal")
    void productos() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
        String json = "{\"nombre\":\"Collar\",\"precio\":5990,\"stock\":5,\"categoria\":\"Accesorios\"}";
        mvc.perform(post("/api/productos").with(httpBasic("camila@mail.cl", PASS))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/productos").with(httpBasic("admin@petsalud.cl", PASS))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
        mvc.perform(get("/api/productos/stock-bajo?cantidad=5").with(httpBasic("camila@mail.cl", PASS)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Expedientes: CLIENTE no crea; VETERINARIO crea a su nombre; la duena ve los de su mascota")
    void expedientes() throws Exception {
        String json = "{\"descripcion\":\"Control\",\"diagnostico\":\"Sano\",\"tratamiento\":\"Ninguno\"}";
        String url = "/api/expedientes?mascotaId=" + mascotaCamila.getId() + "&veterinarioId=" + vet.getId();
        mvc.perform(post(url).with(httpBasic("camila@mail.cl", PASS))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());
        mvc.perform(post(url).with(httpBasic("vet@petsalud.cl", PASS))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
        mvc.perform(get("/api/expedientes/mascota/" + mascotaCamila.getId()).with(httpBasic("camila@mail.cl", PASS)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/expedientes/mascota/" + mascotaCamila.getId()).with(httpBasic("tomas@mail.cl", PASS)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ Autorizacion por propiedad (IDOR)

    @Test
    @DisplayName("IDOR: Camila ve su mascota (200) pero no la de Tomas (403); el veterinario ve ambas")
    void idorMascotas() throws Exception {
        mvc.perform(get("/api/mascotas/" + mascotaCamila.getId()).with(httpBasic("camila@mail.cl", PASS)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/mascotas/" + mascotaTomas.getId()).with(httpBasic("camila@mail.cl", PASS)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/mascotas/propietario/" + tomas.getId()).with(httpBasic("camila@mail.cl", PASS)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/mascotas/" + mascotaTomas.getId()).with(httpBasic("vet@petsalud.cl", PASS)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IDOR: no se agenda para mascota ajena ni se ven ventas, notificaciones o perfil de otro")
    void idorCitasVentasNotificaciones() throws Exception {
        String fecha = "2026-11-10T10:00:00";
        mvc.perform(post("/api/citas").with(httpBasic("camila@mail.cl", PASS))
                        .param("mascotaId", mascotaTomas.getId().toString())
                        .param("veterinarioId", vet.getId().toString())
                        .param("fechaHora", fecha).param("motivo", "Vacuna"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/citas").with(httpBasic("camila@mail.cl", PASS))
                        .param("mascotaId", mascotaCamila.getId().toString())
                        .param("veterinarioId", vet.getId().toString())
                        .param("fechaHora", fecha).param("motivo", "Vacuna"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/ventas/usuario/" + tomas.getId()).with(httpBasic("camila@mail.cl", PASS)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/notificaciones/usuario/" + tomas.getId()).with(httpBasic("camila@mail.cl", PASS)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/" + tomas.getId()).with(httpBasic("camila@mail.cl", PASS)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/" + camila.getId()).with(httpBasic("camila@mail.cl", PASS)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Una cita no puede asignarse a un usuario que no es veterinario -> 400")
    void citaConFalsoVeterinario() throws Exception {
        mvc.perform(post("/api/citas").with(httpBasic("camila@mail.cl", PASS))
                        .param("mascotaId", mascotaCamila.getId().toString())
                        .param("veterinarioId", tomas.getId().toString())
                        .param("fechaHora", "2026-11-10T10:00:00").param("motivo", "Vacuna"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ Exposicion de datos y minimo privilegio

    @Test
    @DisplayName("Listar usuarios con mascotas no genera un JSON infinito (ciclo Usuario-Mascota)")
    void sinCicloJson() throws Exception {
        mvc.perform(get("/api/usuarios").with(httpBasic("admin@petsalud.cl", PASS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mascotas").doesNotExist());
        mvc.perform(get("/api/mascotas/" + mascotaCamila.getId()).with(httpBasic("camila@mail.cl", PASS)))
                .andExpect(jsonPath("$.propietario.mascotas").doesNotExist())
                .andExpect(jsonPath("$.propietario.password").doesNotExist());
    }

    @Test
    @DisplayName("Minimo privilegio: una ruta no declarada se rechaza (403) incluso para el ADMIN")
    void rutaNoDeclaradaDenegada() throws Exception {
        mvc.perform(get("/api/interna/config").with(httpBasic("admin@petsalud.cl", PASS)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cabeceras de seguridad presentes")
    void cabeceras() throws Exception {
        mvc.perform(get("/api/productos"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }
}
