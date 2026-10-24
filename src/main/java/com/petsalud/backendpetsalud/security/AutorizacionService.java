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

    public void verificarPropietarioOAdmin(Long propietarioId) {
        Usuario usuario = obtenerUsuarioAutenticado();

        if (usuario.getRol() == Rol.ADMIN) {
            return;
        }

        if (usuario.getRol() != Rol.CLIENTE
                || !usuario.getId().equals(propietarioId)) {
            throw new AccessDeniedException(
                    "No tiene permiso para acceder a este recurso");
        }
    }
}
