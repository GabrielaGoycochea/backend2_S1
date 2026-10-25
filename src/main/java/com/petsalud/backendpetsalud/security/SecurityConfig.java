package com.petsalud.backendpetsalud.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Seguridad del backend de Clinica PetSalud.
 * Autenticacion: usuario (email) y contrasena con HTTP Basic, usuarios en BD y BCrypt.
 * Autorizacion: por rol. Equivalencias: CLIENTE = cliente, VETERINARIO = empleado, ADMIN = gerente.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String VETERINARIO = "VETERINARIO";

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API stateless: cada peticion se autentica con el encabezado Authorization,
                // sin sesion ni formulario, por lo que CSRF se desactiva.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Publico: registro y catalogo
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
                .httpBasic(withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
