package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Collection;

public class UsuarioDetails implements UserDetails {

    private final Usuario usuario;
    private final boolean bloqueado;

    public UsuarioDetails(Usuario usuario) {
        this(usuario, false);
    }

    public UsuarioDetails(Usuario usuario, boolean bloqueado) {
        this.usuario = usuario;
        this.bloqueado = bloqueado;
    }

    /** Id del usuario autenticado: se usa para verificar la propiedad de los recursos. */
    public Long getId() {
        return usuario.getId();
    }

    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(usuario.getRol().name()));
    }

    @Override
    public String getPassword() {
        return usuario.getPassword();
    }

    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // implementar lógica adicional (solo si es necesario)
    }

    @Override
    public boolean isAccountNonLocked() {
        return !bloqueado; // false tras superar el máximo de intentos fallidos (LoginAttemptService)
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // implementar lógica adicional (solo si es necesario)
    }

    @Override
    public boolean isEnabled() {
        return true; // implementar lógica adicional (solo si es necesario)
    }
}
