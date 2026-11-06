package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.dto.MascotaRequest;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.security.ReglasAcceso;
import com.petsalud.backendpetsalud.service.MascotaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mascotas")
public class MascotaController {

    @Autowired
    private MascotaService mascotaService;

    @PreAuthorize(ReglasAcceso.AUTENTICADO)
    @PostMapping("/propietario/{propietarioId}")
    public ResponseEntity<Mascota> registrarMascota(@PathVariable Long propietarioId,
                                                    @Valid @RequestBody MascotaRequest datos) {
        Mascota nuevaMascota = mascotaService.registrarMascota(datos.aEntidad(), propietarioId);
        return ResponseEntity.ok(nuevaMascota);
    }

    @PreAuthorize(ReglasAcceso.AUTENTICADO)
    @GetMapping("/{id}")
    public ResponseEntity<Mascota> obtenerMascotaPorId(@PathVariable Long id) {
        return mascotaService.obtenerMascotaPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize(ReglasAcceso.AUTENTICADO)
    @PutMapping("/{id}")
    public ResponseEntity<Mascota> actualizarMascota(@PathVariable Long id, @Valid @RequestBody MascotaRequest datos) {
        Mascota mascotaActualizada = mascotaService.actualizarMascota(id, datos.aEntidad());
        return ResponseEntity.ok(mascotaActualizada);
    }

    @PreAuthorize(ReglasAcceso.AUTENTICADO)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarMascota(@PathVariable Long id) {
        mascotaService.eliminarMascota(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(ReglasAcceso.AUTENTICADO)
    @GetMapping("/propietario/{propietarioId}")
    public ResponseEntity<List<Mascota>> listarMascotasPorPropietario(@PathVariable Long propietarioId) {
        List<Mascota> mascotas = mascotaService.listarMascotasPorPropietario(propietarioId);
        return ResponseEntity.ok(mascotas);
    }
}
