package com.petsalud.backendpetsalud.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad del backend de Clinica PetSalud.
 * Autenticacion: login con email y contrasena (BCrypt) que entrega un JWT; luego cada
 * peticion se autentica con el header "Authorization: Bearer <token>" (Semana 2).
 * Autorizacion: por rol. Equivalencias: CLIENTE = cliente, VETERINARIO = empleado, ADMIN = gerente.
 * Semana 3: ademas de las reglas por URL, cada endpoint declara su regla con @PreAuthorize
 * (ver ReglasAcceso). @EnableMethodSecurity activa esas anotaciones.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String VETERINARIO = "VETERINARIO";

    private final UsuarioDetailsService usuarioDetailsService;
    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService, JwtRequestFilter jwtRequestFilter) {
        this.usuarioDetailsService = usuarioDetailsService;
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API stateless: cada peticion se autentica con el encabezado Authorization,
                // sin sesion ni formulario, por lo que CSRF se desactiva.
                .csrf(AbstractHttpConfigurer::disable)
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

                        // Empleado y gerente: expedientes y gestion de citas
                        .requestMatchers("/api/expedientes/**").hasAnyAuthority(VETERINARIO, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/citas/entre-fechas").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/citas/veterinario/**")
                        .hasAnyAuthority(VETERINARIO, ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/citas/*/estado").hasAnyAuthority(VETERINARIO, ADMIN)

                        // Resto: cualquier usuario autenticado
                        .anyRequest().authenticated()
                )
                .userDetailsService(usuarioDetailsService)
                // Sin token o con token invalido se responde 401 (sin permiso por rol: 403)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // El filtro JWT se ejecuta antes de la autenticacion estandar de Spring Security
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
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