package com.petsalud.backendpetsalud.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepta cada solicitud, lee el header "Authorization: Bearer <token>" y, si el token
 * es valido, autentica al usuario en el SecurityContext. Se ejecuta antes de
 * UsernamePasswordAuthenticationFilter (ver SecurityConfig).
 * Semana 3: cada token rechazado (alterado, vencido, de otro emisor o audiencia, o de un
 * usuario inexistente) queda registrado como JWT_INVALIDO en el log de seguridad.
 */
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioDetailsService usuarioDetailsService;
    private final ActividadSospechosaService actividad;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String username = jwtUtil.extraerUsername(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails usuario = usuarioDetailsService.loadUserByUsername(username);
                    if (jwtUtil.validarToken(token, usuario)) {
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                usuario, null, usuario.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        actividad.jwtInvalido(request, "token_de_otro_usuario_o_sin_expiracion");
                    }
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
                SecurityContextHolder.clearContext();
                // Solo el tipo de error (ExpiredJwtException, SignatureException...), nunca el token
                actividad.jwtInvalido(request, e.getClass().getSimpleName());
            }
        }

        filterChain.doFilter(request, response);
    }
}