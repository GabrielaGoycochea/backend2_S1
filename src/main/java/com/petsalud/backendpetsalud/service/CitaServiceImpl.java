package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.CitaRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.service.CitaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CitaServiceImpl implements CitaService {

    /** Lista blanca de estados válidos (no se acepta texto libre del cliente). */
    private static final Set<String> ESTADOS_VALIDOS = Set.of("Agendada", "Confirmada", "Completada", "Cancelada");

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public Cita agendarCita(Long mascotaId, Long veterinarioId, LocalDateTime fechaHora, String motivo) {
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));

        // Solo se puede asignar a un usuario que realmente sea veterinario
        Usuario veterinario = usuarioRepository.findById(veterinarioId)
                .filter(u -> u.getRol() == Rol.VETERINARIO)
                .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));

        if (motivo == null || motivo.isBlank() || motivo.length() > 255) {
            throw new IllegalArgumentException("El motivo es obligatorio (máximo 255 caracteres)");
        }

        // Opcional: Verificar disponibilidad del veterinario en ese horario

        Cita cita = Cita.builder()
                .mascota(mascota)
                .veterinario(veterinario)
                .fechaHora(fechaHora)
                .motivo(motivo)
                .estado("Agendada")
                .build();

        return citaRepository.save(cita);
    }

    @Override
    public Optional<Cita> obtenerCitaPorId(Long id) {
        return citaRepository.findById(id);
    }

    @Override
    public Cita actualizarEstadoCita(Long id, String nuevoEstado) {
        if (!ESTADOS_VALIDOS.contains(nuevoEstado)) {
            throw new IllegalArgumentException("Estado inválido. Valores permitidos: " + ESTADOS_VALIDOS);
        }
        return citaRepository.findById(id).map(cita -> {
            cita.setEstado(nuevoEstado);
            return citaRepository.save(cita);
        }).orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
    }

    @Override
    public void cancelarCita(Long id) {
        citaRepository.deleteById(id);
    }

    @Override
    public List<Cita> listarCitasPorMascota(Long mascotaId) {
        return citaRepository.findByMascotaId(mascotaId);
    }

    @Override
    public List<Cita> listarCitasPorVeterinario(Long veterinarioId) {
        return citaRepository.findByVeterinarioId(veterinarioId);
    }

    @Override
    public List<Cita> listarCitasEntreFechas(LocalDateTime inicio, LocalDateTime fin) {
        return citaRepository.findByFechaHoraBetween(inicio, fin);
    }
}

