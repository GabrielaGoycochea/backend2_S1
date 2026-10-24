package com.petsalud.backendpetsalud.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Seguridad del backend de Clinica PetSalud.
 * Autenticacion: usuario (email) y contrasena con HTTP Basic, usuarios en BD y BCrypt.
 * Autorizacion en dos capas:
 *  1) Reglas por URL y verbo HTTP en este SecurityFilterChain (rol).
 *  2) @PreAuthorize en los controladores (rol + propiedad del recurso, ver SeguridadService).
 * Equivalencias: CLIENTE = cliente, VETERINARIO = empleado, ADMIN = gerente.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize en los controladores
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
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        // Publico: registro y catalogo
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/registro").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/productos/stock-bajo").hasAnyAuthority(VETERINARIO, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/**").permitAll()

                        // Gerente: gestion de usuarios y productos, listado total de ventas
                        .requestMatchers(HttpMethod.GET, "/api/usuarios").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/**").hasAuthority(ADMIN)
                        .requestMatchers("/api/productos/**").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/ventas").hasAuthority(ADMIN)

                        // Empleado y gerente: escritura clinica y gestion de citas
                        .requestMatchers(HttpMethod.POST, "/api/expedientes").hasAnyAuthority(VETERINARIO, ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/expedientes/**").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/citas/entre-fechas", "/api/citas/veterinario/**")
                                .hasAnyAuthority(VETERINARIO, ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/citas/*/estado").hasAnyAuthority(VETERINARIO, ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/recompensas/**", "/api/notificaciones/**")
                                .hasAnyAuthority(VETERINARIO, ADMIN)

                        // Recursos del negocio: requieren autenticacion y luego @PreAuthorize
                        // verifica que el recurso sea del usuario (o que sea personal de la clinica)
                        .requestMatchers("/api/auth/me", "/api/usuarios/**", "/api/mascotas/**", "/api/citas/**",
                                "/api/expedientes/**", "/api/ventas/**", "/api/recompensas/**", "/api/notificaciones/**")
                                .authenticated()

                        // Minimo privilegio: cualquier ruta no declarada queda bloqueada
                        .anyRequest().denyAll()
                )
                .userDetailsService(usuarioDetailsService)
                .httpBasic(withDefaults())
                // Cabeceras de seguridad (XSS, clickjacking, sniffing, HTTPS)
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Publica los eventos de "acceso denegado" para registrarlos en SecurityEventsListener. */
    @Bean
    public AuthorizationEventPublisher authorizationEventPublisher(ApplicationEventPublisher publisher) {
        return new SpringAuthorizationEventPublisher(publisher);
    }
}
