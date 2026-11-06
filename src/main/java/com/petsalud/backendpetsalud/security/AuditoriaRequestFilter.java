package com.petsalud.backendpetsalud.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Filtro de auditoria (Semana 3). Va en la cadena de Spring Security antes del filtro JWT:
 * 1. Cuenta cada solicitud por IP para detectar rafagas (ActividadSospechosaService).
 * 2. Cuando la respuesta ya esta lista, registra las escrituras exitosas (POST, PUT, PATCH, DELETE)
 *    con el usuario que las hizo: quien creo, modifico o elimino.
 *
 * No es un @Component a proposito: se crea en SecurityConfig para que se ejecute solo dentro
 * de la cadena de seguridad y no dos veces.
 */
public class AuditoriaRequestFilter extends OncePerRequestFilter {

    private static final Set<String> ESCRITURAS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final ActividadSospechosaService actividad;

    public AuditoriaRequestFilter(ActividadSospechosaService actividad) {
        this.actividad = actividad;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        actividad.registrarSolicitud(request);
        try {
            filterChain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            if (ESCRITURAS.contains(request.getMethod())
                    && !request.getRequestURI().startsWith("/api/auth/")
                    && status >= 200 && status < 300) {
                actividad.operacion(request, usuarioActual(), status);
            }
        }
    }

    static String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return "anonimo";
        }
        return auth.getName();
    }
}
