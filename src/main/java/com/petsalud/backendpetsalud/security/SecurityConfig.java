package com.petsalud.backendpetsalud.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Seguridad del backend de Clinica PetSalud.
 * Autenticacion: login con email y contrasena (BCrypt) que entrega un JWT; luego cada
 * peticion se autentica con el header "Authorization: Bearer <token>" (Semana 2).
 * Autorizacion: por rol. Equivalencias: CLIENTE = cliente, VETERINARIO = empleado, ADMIN = gerente.
 * Semana 3: ademas de las reglas por URL, cada endpoint declara su regla con @PreAuthorize
 * (ver ReglasAcceso). @EnableMethodSecurity activa esas anotaciones.
 * Semana 3: los 401 y 403 quedan registrados por ActividadSospechosaService y el filtro de
 * auditoria registra rafagas de solicitudes y escrituras exitosas. Se agregan cabeceras contra
 * XSS (Content-Security-Policy) y CORS solo para los origenes configurados.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String VETERINARIO = "VETERINARIO";

    private final UsuarioDetailsService usuarioDetailsService;
    private final JwtRequestFilter jwtRequestFilter;
    private final ActividadSospechosaService actividad;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService, JwtRequestFilter jwtRequestFilter,
                          ActividadSospechosaService actividad) {
        this.usuarioDetailsService = usuarioDetailsService;
        this.jwtRequestFilter = jwtRequestFilter;
        this.actividad = actividad;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF: un ataque CSRF necesita que el navegador adjunte la credencial por su cuenta
                // (cookie de sesion). Aqui no hay cookies ni sesion: el JWT viaja en el header
                // Authorization, que el navegador nunca agrega solo y que otro sitio no puede leer.
                // Por eso el token CSRF se desactiva, y se refuerza con CORS restringido (ver abajo).
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                // Cabeceras contra XSS y clickjacking. Spring ya agrega X-Content-Type-Options: nosniff,
                // X-Frame-Options: DENY y Cache-Control sin cache; se suma una CSP que impide ejecutar
                // scripts si una respuesta llegara a abrirse en un navegador.
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Semana 2: se deshabilita la autenticacion por defecto (HTTP Basic y formulario)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Publico: login, registro y catalogo
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/registro").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/productos/stock-bajo").hasAnyAuthority(VETERINARIO, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/**").permitAll()

                        // Gerente: usuarios, gestion de productos y listado total de ventas
                        .requestMatchers("/api/usuarios/**").hasAuthority(ADMIN)
                        .requestMatchers("/api/productos/**").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/ventas").hasAuthority(ADMIN)

                        // Empleado y gerente: expedientes y gestion de citas.
                        // Eliminar un expediente medico queda solo para el gerente (Semana 3)
                        .requestMatchers(HttpMethod.DELETE, "/api/expedientes/**").hasAuthority(ADMIN)
                        .requestMatchers("/api/expedientes/**").hasAnyAuthority(VETERINARIO, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/citas/entre-fechas").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/citas/veterinario/**")
                        .hasAnyAuthority(VETERINARIO, ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/citas/*/estado").hasAnyAuthority(VETERINARIO, ADMIN)

                        // Resto: cualquier usuario autenticado
                        .anyRequest().authenticated()
                )
                .userDetailsService(usuarioDetailsService)
                // Sin token o con token invalido se responde 401 (sin permiso por rol: 403).
                // Semana 3: ambos casos quedan en el log de seguridad.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            actividad.sinAutenticacion(request);
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            actividad.accesoDenegado(request, AuditoriaRequestFilter.usuarioActual());
                            new AccessDeniedHandlerImpl().handle(request, response, e);
                        }))
                // El filtro JWT se ejecuta antes de la autenticacion estandar de Spring Security
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                // Auditoria: va antes del filtro JWT para contar todas las solicitudes y, al final,
                // registrar las escrituras exitosas con el usuario ya autenticado
                .addFilterBefore(new AuditoriaRequestFilter(actividad), JwtRequestFilter.class);

        return http.build();
    }

    /**
     * CORS: solo los front autorizados (cors.allowed-origins) pueden llamar a la API desde un navegador.
     * allowCredentials=false porque la API no usa cookies: el navegador nunca envia credenciales solas.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins:http://localhost:3000}") String[] origenesPermitidos) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origenesPermitidos));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Usado por AuthController para validar email y contrasena en el login
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}