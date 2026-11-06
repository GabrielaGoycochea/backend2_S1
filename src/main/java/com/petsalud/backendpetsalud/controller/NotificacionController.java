package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.dto.NotificacionRequest;
import com.petsalud.backendpetsalud.entity.Notificacion;
import com.petsalud.backendpetsalud.security.ReglasAcceso;
import com.petsalud.backendpetsalud.service.NotificacionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    @PreAuthorize(ReglasAcceso.GERENTE)
    @PostMapping("/usuario/{usuarioId}")
    public ResponseEntity<Notificacion> enviarNotificacion(@PathVariable Long usuarioId,
                                                           @Valid @RequestBody NotificacionRequest datos) {
        Notificacion nuevaNotificacion = notificacionService.enviarNotificacion(usuarioId, datos.aEntidad());
        return ResponseEntity.ok(nuevaNotificacion);
    }

    @PreAuthorize(ReglasAcceso.AUTENTICADO)
    @GetMapping("/{id}")
    public ResponseEntity<Notificacion> obtenerNotificacionPorId(@PathVariable Long id) {
        return notificacionService.obtenerNotificacionPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize(ReglasAcceso.AUTENTICADO)
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Notificacion>> listarNotificacionesPorUsuario(@PathVariable Long usuarioId) {
        List<Notificacion> notificaciones = notificacionService.listarNotificacionesPorUsuario(usuarioId);
        return ResponseEntity.ok(notificaciones);
    }

    @PreAuthorize(ReglasAcceso.AUTENTICADO)
    @GetMapping("/usuario/{usuarioId}/no-leidas")
    public ResponseEntity<List<Notificacion>> listarNotificacionesNoLeidas(@PathVariable Long usuarioId) {
        List<Notificacion> notificaciones = notificacionService.listarNotificacionesNoLeidas(usuarioId);
        return ResponseEntity.ok(notificaciones);
    }

    @PreAuthorize(ReglasAcceso.AUTENTICADO)
    @PutMapping("/{id}/leida")
    public ResponseEntity<Void> marcarComoLeida(@PathVariable Long id) {
        notificacionService.marcarComoLeida(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(ReglasAcceso.AUTENTICADO)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarNotificacion(@PathVariable Long id) {
        notificacionService.eliminarNotificacion(id);
        return ResponseEntity.noContent().build();
    }
}
