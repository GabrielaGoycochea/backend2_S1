package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.security.ReglasAcceso;
import org.springframework.security.access.prepost.PreAuthorize;
import com.petsalud.backendpetsalud.dto.AuthRequest;
import com.petsalud.backendpetsalud.dto.AuthResponse;
import com.petsalud.backendpetsalud.security.ActividadSospechosaService;
import com.petsalud.backendpetsalud.security.JwtUtil;
import com.petsalud.backendpetsalud.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;
    private final ActividadSospechosaService actividad;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                          LoginAttemptService loginAttemptService, ActividadSospechosaService actividad) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.loginAttemptService = loginAttemptService;
        this.actividad = actividad;
    }

    // Autenticacion: valida email y contrasena y, si son correctos, devuelve un JWT.
    // Tras 5 intentos fallidos el email se bloquea 5 minutos (429 Too Many Requests).
    // Semana 3: cada intento queda en el log de seguridad con IP y fallos seguidos (nunca la contrasena).
    @PreAuthorize(ReglasAcceso.PUBLICO)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletRequest http) {
        if (request == null || request.email() == null || request.email().isBlank()
                || request.password() == null) {
            actividad.loginFallido(http, null, 0);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales invalidas");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        long espera = loginAttemptService.segundosRestantes(email);
        if (espera > 0) {
            long minutos = (espera + 59) / 60;
            actividad.loginDuranteBloqueo(http, email);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(espera))
                    .body("Cuenta bloqueada temporalmente por demasiados intentos fallidos. "
                            + "Intente nuevamente en " + minutos + " minuto(s).");
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
            loginAttemptService.registrarExito(email);
            actividad.loginExitoso(http, email);
            String token = jwtUtil.generarToken((UserDetails) auth.getPrincipal());
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (AuthenticationException e) {
            boolean bloqueada = loginAttemptService.registrarFallo(email);
            actividad.loginFallido(http, email, loginAttemptService.fallosRegistrados(email));
            if (bloqueada) {
                actividad.cuentaBloqueada(http, email, loginAttemptService.minutosBloqueo());
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales invalidas");
        }
    }
}
