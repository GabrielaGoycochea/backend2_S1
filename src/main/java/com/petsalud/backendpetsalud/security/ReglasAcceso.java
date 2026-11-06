package com.petsalud.backendpetsalud.security;

/**
 * Reglas de autorizacion por metodo, usadas con {@code @PreAuthorize} en los controladores.
 * Equivalencia de roles del caso: CLIENTE = cliente, VETERINARIO = empleado, ADMIN = gerente.
 *
 * Estas reglas complementan (no reemplazan) las reglas por URL de {@link SecurityConfig}:
 * si una ruta se configura mal, la anotacion en el metodo igual bloquea el acceso (defensa en profundidad).
 * La propiedad del recurso (que un cliente solo vea lo suyo) se sigue validando en los servicios
 * con {@link AutorizacionService}, porque depende del dato consultado y no solo del rol.
 */
public final class ReglasAcceso {

    /** Visitante: cualquier persona, sin token (login, registro y catalogo). */
    public static final String PUBLICO = "permitAll()";

    /** Cualquier rol con JWT valido; el servicio comprueba ademas que el recurso sea suyo. */
    public static final String AUTENTICADO = "isAuthenticated()";

    /** Personal de la clinica: empleado (VETERINARIO) o gerente (ADMIN). */
    public static final String PERSONAL = "hasAnyAuthority('VETERINARIO', 'ADMIN')";

    /** Solo gerente (ADMIN). */
    public static final String GERENTE = "hasAuthority('ADMIN')";

    private ReglasAcceso() {
    }
}
