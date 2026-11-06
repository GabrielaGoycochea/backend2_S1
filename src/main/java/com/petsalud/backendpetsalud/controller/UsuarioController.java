package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.security.ReglasAcceso;
import org.springframework.security.access.prepost.PreAuthorize;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.dto.RegistroUsuarioRequest;
import jakarta.validation.Valid;
import com.petsalud.backendpetsalud.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PreAuthorize(ReglasAcceso.PUBLICO)
    @PostMapping("/registro")
    public ResponseEntity<Usuario> registrarUsuario(
            @Valid @RequestBody RegistroUsuarioRequest datos) {

        Usuario usuario = Usuario.builder()
                .email(datos.email().trim().toLowerCase(java.util.Locale.ROOT))
                .password(datos.password())
                .nombre(datos.nombre())
                .apellido(datos.apellido())
                .telefono(datos.telefono())
                .direccion(datos.direccion())
                .build();

        Usuario nuevoUsuario = usuarioService.registrarUsuario(usuario);
        return ResponseEntity.ok(nuevoUsuario);
    }

    @PreAuthorize(ReglasAcceso.GERENTE)
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerUsuarioPorId(@PathVariable Long id) {
        return usuarioService.obtenerUsuarioPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize(ReglasAcceso.GERENTE)
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizarUsuario(@PathVariable Long id, @RequestBody Usuario usuario) {
        Usuario usuarioActualizado = usuarioService.actualizarUsuario(id, usuario);
        return ResponseEntity.ok(usuarioActualizado);
    }

    @PreAuthorize(ReglasAcceso.GERENTE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(ReglasAcceso.GERENTE)
    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        return ResponseEntity.ok(usuarios);
    }
}
