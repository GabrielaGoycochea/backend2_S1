package com.petsalud.backendpetsalud.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.petsalud.backendpetsalud.dto.AuthRequest;
import com.petsalud.backendpetsalud.dto.AuthResponse;
import com.petsalud.backendpetsalud.security.JwtUtil;
import com.petsalud.backendpetsalud.security.LoginAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                          LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.loginAttemptService = loginAttemptService;
    }

    // Autenticacion: valida email y contrasena y, si son correctos, devuelve un JWT.
    // Tras 5 intentos fallidos desde una misma IP, esa combinacion de correo e IP se bloquea
    // 5 minutos (429 Too Many Requests). Asi quien ataca desde otra IP no bloquea al usuario real.
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.email() == null || request.email().isBlank()
                || request.password() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales invalidas");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        String clave = email + "|" + httpRequest.getRemoteAddr(); // correo + IP
        long espera = loginAttemptService.segundosRestantes(clave);
        if (espera > 0) {
            long minutos = (espera + 59) / 60;
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(espera))
                    .body("Cuenta bloqueada temporalmente por demasiados intentos fallidos. "
                            + "Intente nuevamente en " + minutos + " minuto(s).");
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
            loginAttemptService.registrarExito(clave);
            String token = jwtUtil.generarToken((UserDetails) auth.getPrincipal());
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (AuthenticationException e) {
            if (loginAttemptService.registrarFallo(clave)) {
                log.warn("CUENTA BLOQUEADA usuario={} ip={} por intentos de login fallidos",
                        email, httpRequest.getRemoteAddr());
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales invalidas");
        }
    }
}
