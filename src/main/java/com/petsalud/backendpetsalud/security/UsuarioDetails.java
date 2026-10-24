package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Collection;

public class UsuarioDetails implements UserDetails {

    private final Usuario usuario;

    public UsuarioDetails(Usuario usuario) {
        this.usuario = usuario;
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
        return true; // implementar lógica adicional (solo si es necesario)
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
