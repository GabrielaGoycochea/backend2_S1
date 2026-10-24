package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.security.UsuarioDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** Permite comprobar las credenciales ("login") y ver con que usuario y rol quedo autenticada la peticion. */
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal UsuarioDetails detalles) {
        Usuario u = detalles.getUsuario();
        return Map.of(
                "id", u.getId(),
                "email", u.getEmail(),
                "nombre", u.getNombre() == null ? "" : u.getNombre(),
                "rol", u.getRol().name());
    }
}
