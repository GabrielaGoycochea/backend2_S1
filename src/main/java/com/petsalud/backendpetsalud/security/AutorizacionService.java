package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AutorizacionService {

    private final UsuarioRepository usuarioRepository;

    public AutorizacionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obtenerUsuarioAutenticado() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("Acceso no autorizado");
        }

        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Usuario autenticado no encontrado"));
    }

    /**
     * Permite al ADMIN o al dueno del recurso (el usuario autenticado cuyo id coincide),
     * sin importar su rol. Un VETERINARIO solo accede asi a lo que es suyo.
     * Se usa para modificar, eliminar y consultar datos personales (ventas, notificaciones).
     */
    public void verificarPropietarioOAdmin(Long propietarioId) {
        Usuario usuario = obtenerUsuarioAutenticado();

        if (usuario.getRol() == Rol.ADMIN) {
            return;
        }

        if (propietarioId == null || !usuario.getId().equals(propietarioId)) {
            throw new AccessDeniedException(
                    "No tiene permiso para acceder a este recurso");
        }
    }

    /**
     * Permite al personal (ADMIN y VETERINARIO) o al dueno del recurso.
     * Se usa para consultar y registrar (mascotas, recompensas) y para registrar ventas.
     */
    public void verificarPersonalOPropietario(Long propietarioId) {
        Usuario usuario = obtenerUsuarioAutenticado();

        if (usuario.getRol() == Rol.ADMIN || usuario.getRol() == Rol.VETERINARIO) {
            return;
        }

        if (propietarioId == null || !usuario.getId().equals(propietarioId)) {
            throw new AccessDeniedException(
                    "No tiene permiso para acceder a este recurso");
        }
    }

    /** Solo el ADMIN (gerente). Se usa para crear y eliminar recompensas. */
    public void verificarAdmin() {
        Usuario usuario = obtenerUsuarioAutenticado();

        if (usuario.getRol() != Rol.ADMIN) {
            throw new AccessDeniedException(
                    "Solo un administrador puede realizar esta operacion");
        }
    }

    /**
     * El ADMIN, o el VETERINARIO autenticado cuando el id indicado es el suyo.
     * Evita registrar expedientes medicos a nombre de otro veterinario.
     */
    public void verificarIdentidadVeterinario(Long veterinarioId) {
        Usuario usuario = obtenerUsuarioAutenticado();

        if (usuario.getRol() == Rol.ADMIN) {
            return;
        }

        if (usuario.getRol() == Rol.VETERINARIO && usuario.getId().equals(veterinarioId)) {
            return;
        }

        throw new AccessDeniedException(
                "Solo puede registrar expedientes con su propia identidad");
    }
}
