package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.controller.AuthController;
import com.petsalud.backendpetsalud.controller.ProductoController;
import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterio 2 (monitoreo basico de actividad sospechosa): cada evento de seguridad queda
 * registrado con IP, usuario y operacion, sin contrasenas ni tokens.
 */
@ExtendWith(OutputCaptureExtension.class)
@WebMvcTest(controllers = {AuthController.class, ProductoController.class})
@Import({SecurityConfig.class, JwtRequestFilter.class, ActividadSospechosaService.class, JwtUtil.class, LoginAttemptService.class})
@TestPropertySource(properties = {"jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "jwt.expiration-ms=900000", "jwt.issuer=PetSalud"})
class MonitoreoSeguridadTest {

    private static final String PRODUCTO =
            "{\"nombre\":\"Collar de cuero\",\"descripcion\":\"Collar ajustable\",\"precio\":9990,\"stock\":10,\"categoria\":\"Accesorios\"}";

    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder encoder;
    @MockBean private UsuarioDetailsService usuarios;
    @MockBean private ProductoService productos;

    private void prepararUsuario(String email) {
        String hash = encoder.encode("Clave1234");
        when(usuarios.loadUserByUsername(email)).thenAnswer(inv ->
                User.withUsername(email).password(hash).authorities("CLIENTE").build());
    }

    private ResultActions login(String email, String clave) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + clave + "\"}"));
    }

    @Test
    void loginFallidoQuedaRegistradoConIpYSinContrasena(CapturedOutput salida) throws Exception {
        prepararUsuario("monitoreo1@petsalud.cl");
        login("monitoreo1@petsalud.cl", "ClaveIncorrecta99").andExpect(status().isUnauthorized());

        assertThat(salida.getOut())
                .contains("LOGIN_FALLIDO usuario=monitoreo1@petsalud.cl ip=127.0.0.1 fallosSeguidos=1")
                .doesNotContain("ClaveIncorrecta99");
    }

    @Test
    void quintoFalloRegistraCuentaBloqueada(CapturedOutput salida) throws Exception {
        prepararUsuario("monitoreo2@petsalud.cl");
        for (int i = 0; i < 5; i++) {
            login("monitoreo2@petsalud.cl", "Incorrecta" + i).andExpect(status().isUnauthorized());
        }
        login("monitoreo2@petsalud.cl", "Clave1234").andExpect(status().isTooManyRequests());

        assertThat(salida.getOut())
                .contains("LOGIN_FALLIDO usuario=monitoreo2@petsalud.cl ip=127.0.0.1 fallosSeguidos=5")
                .contains("CUENTA_BLOQUEADA usuario=monitoreo2@petsalud.cl ip=127.0.0.1 minutos=5")
                .contains("LOGIN_RECHAZADO_BLOQUEO usuario=monitoreo2@petsalud.cl");
    }

    @Test
    void loginCorrectoQuedaRegistrado(CapturedOutput salida) throws Exception {
        prepararUsuario("monitoreo3@petsalud.cl");
        login("monitoreo3@petsalud.cl", "Clave1234").andExpect(status().isOk());
        assertThat(salida.getOut()).contains("LOGIN_OK usuario=monitoreo3@petsalud.cl ip=127.0.0.1");
    }

    @Test
    void jwtAlteradoQuedaRegistradoSinMostrarElToken(CapturedOutput salida) throws Exception {
        mvc.perform(get("/api/productos/stock-bajo").param("cantidad", "5")
                        .header("Authorization", "Bearer token.alterado.xyz"))
                .andExpect(status().isUnauthorized());

        assertThat(salida.getOut())
                .contains("JWT_INVALIDO ip=127.0.0.1 GET /api/productos/stock-bajo motivo=")
                .contains("SIN_AUTENTICACION ip=127.0.0.1 GET /api/productos/stock-bajo")
                .doesNotContain("token.alterado.xyz");
    }

    @Test
    @WithMockUser(username = "cliente@petsalud.cl", authorities = "CLIENTE")
    void accesoDenegadoQuedaRegistrado(CapturedOutput salida) throws Exception {
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
                .andExpect(status().isForbidden());
        assertThat(salida.getOut()).contains("ACCESO_DENEGADO usuario=cliente@petsalud.cl ip=127.0.0.1 POST /api/productos");
    }

    @Test
    @WithMockUser(username = "admin@petsalud.cl", authorities = "ADMIN")
    void escrituraExitosaQuedaAuditada(CapturedOutput salida) throws Exception {
        when(productos.agregarProducto(any(Producto.class))).thenReturn(new Producto());
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
                .andExpect(status().isOk());
        assertThat(salida.getOut()).contains("OPERACION usuario=admin@petsalud.cl ip=127.0.0.1 POST /api/productos status=200");
    }
}
