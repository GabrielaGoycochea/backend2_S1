package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.controller.AuthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtRequestFilter.class, ActividadSospechosaService.class, JwtUtil.class, LoginAttemptService.class})
@TestPropertySource(properties = {"jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "jwt.expiration-ms=900000", "jwt.issuer=PetSalud"})
class AuthLockoutTest {

    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder encoder;
    @MockBean private UsuarioDetailsService usuarios;

    private void prepararUsuario(String email) {
        String hash = encoder.encode("Clave1234");
        // Se devuelve un objeto nuevo en cada llamada, igual que UsuarioDetailsService con la BD.
        // Si se reutiliza la misma instancia, Spring Security borra su password tras el primer
        // login exitoso (eraseCredentials) y el segundo login correcto falla con 401.
        when(usuarios.loadUserByUsername(email)).thenAnswer(inv ->
                User.withUsername(email).password(hash).authorities("CLIENTE").build());
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String clave) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + clave + "\"}"));
    }

    @Test
    void cincoFallosBloqueanYElSextoIntentoDevuelve429AunConClaveCorrecta() throws Exception {
        String email = "bloqueo@petsalud.cl";
        prepararUsuario(email);
        for (int i = 0; i < 5; i++) {
            login(email, "incorrecta").andExpect(status().isUnauthorized());
        }
        login(email, "Clave1234")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void cuatroFallosYLoginCorrectoNoBloquean() throws Exception {
        String email = "sinbloqueo@petsalud.cl";
        prepararUsuario(email);
        for (int i = 0; i < 4; i++) {
            login(email, "incorrecta").andExpect(status().isUnauthorized());
        }
        login(email, "Clave1234").andExpect(status().isOk());
        for (int i = 0; i < 4; i++) {
            login(email, "incorrecta").andExpect(status().isUnauthorized());
        }
        login(email, "Clave1234").andExpect(status().isOk());
    }

    @Test
    void atacanteDesdeOtraIpNoBloqueaAlUsuarioLegitimo() throws Exception {
        String email = "pruebaip@petsalud.cl";
        prepararUsuario(email);
        for (int i = 0; i < 5; i++) {
            loginDesdeIp(email, "incorrecta", "203.0.113.50").andExpect(status().isUnauthorized());
        }
        loginDesdeIp(email, "Clave1234", "203.0.113.50")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        loginDesdeIp(email, "Clave1234", "198.51.100.10").andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions loginDesdeIp(String email, String clave, String ip) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setRemoteAddr(ip); return req; })
                .content("{\"email\":\"" + email + "\",\"password\":\"" + clave + "\"}"));
    }
}
