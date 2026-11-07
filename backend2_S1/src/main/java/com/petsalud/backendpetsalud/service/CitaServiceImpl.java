package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import com.petsalud.backendpetsalud.entity.Mascota;
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

@Service
public class CitaServiceImpl implements CitaService {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AutorizacionService autorizacionService;

    @Override
    public Cita agendarCita(Long mascotaId, Long veterinarioId, LocalDateTime fechaHora, String motivo) {
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));

        autorizacionService.verificarPropietarioOAdmin(
                mascota.getPropietario().getId()
        );

        Usuario veterinario = usuarioRepository.findById(veterinarioId)
                .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));

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
        Optional<Cita> resultado = citaRepository.findById(id);

        if (resultado.isEmpty()) {
            return resultado;
        }

        Cita cita = resultado.get();
        Usuario usuario = autorizacionService.obtenerUsuarioAutenticado();

        boolean esAdmin =
                usuario.getRol() == com.petsalud.backendpetsalud.entity.Rol.ADMIN;

        boolean esPropietario =
                usuario.getRol() == com.petsalud.backendpetsalud.entity.Rol.CLIENTE
                && cita.getMascota() != null
                && cita.getMascota().getPropietario() != null
                && usuario.getId().equals(cita.getMascota().getPropietario().getId());

        boolean esVeterinarioAsignado =
                usuario.getRol() == com.petsalud.backendpetsalud.entity.Rol.VETERINARIO
                && cita.getVeterinario() != null
                && usuario.getId().equals(cita.getVeterinario().getId());

        if (!esAdmin && !esPropietario && !esVeterinarioAsignado) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tiene permiso para consultar esta cita");
        }

        return resultado;
    }

    @Override
    public Cita actualizarEstadoCita(Long id, String nuevoEstado) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));

        Usuario usuario = autorizacionService.obtenerUsuarioAutenticado();

        boolean esAdmin = usuario.getRol() == com.petsalud.backendpetsalud.entity.Rol.ADMIN;

        boolean esVeterinarioAsignado =
                usuario.getRol() == com.petsalud.backendpetsalud.entity.Rol.VETERINARIO
                && cita.getVeterinario() != null
                && usuario.getId().equals(cita.getVeterinario().getId());

        if (!esAdmin && !esVeterinarioAsignado) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tiene permiso para modificar esta cita");
        }

        cita.setEstado(nuevoEstado);
        return citaRepository.save(cita);
    }

    @Override
    public void cancelarCita(Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));

        autorizacionService.verificarPropietarioOAdmin(
                cita.getMascota().getPropietario().getId()
        );

        citaRepository.deleteById(id);
    }

    @Override
    public List<Cita> listarCitasPorMascota(Long mascotaId) {
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));

        Usuario usuario = autorizacionService.obtenerUsuarioAutenticado();

        if (usuario.getRol() == com.petsalud.backendpetsalud.entity.Rol.ADMIN) {
            return citaRepository.findByMascotaId(mascotaId);
        }

        if (usuario.getRol() == com.petsalud.backendpetsalud.entity.Rol.CLIENTE) {
            autorizacionService.verificarPropietarioOAdmin(
                    mascota.getPropietario().getId()
            );
            return citaRepository.findByMascotaId(mascotaId);
        }

        if (usuario.getRol() == com.petsalud.backendpetsalud.entity.Rol.VETERINARIO) {
            return citaRepository.findByMascotaId(mascotaId).stream()
                    .filter(cita -> cita.getVeterinario() != null
                            && usuario.getId().equals(cita.getVeterinario().getId()))
                    .toList();
        }

        throw new org.springframework.security.access.AccessDeniedException(
                "No tiene permiso para consultar estas citas");
    }

    @Override
    public List<Cita> listarCitasPorVeterinario(Long veterinarioId) {
        Usuario usuario = autorizacionService.obtenerUsuarioAutenticado();

        if (usuario.getRol() != com.petsalud.backendpetsalud.entity.Rol.ADMIN
                && (usuario.getRol() != com.petsalud.backendpetsalud.entity.Rol.VETERINARIO
                || !usuario.getId().equals(veterinarioId))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tiene permiso para consultar estas citas");
        }

        return citaRepository.findByVeterinarioId(veterinarioId);
    }

    @Override
    public List<Cita> listarCitasEntreFechas(LocalDateTime inicio, LocalDateTime fin) {
        Usuario usuario = autorizacionService.obtenerUsuarioAutenticado();

        if (usuario.getRol() != com.petsalud.backendpetsalud.entity.Rol.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo un administrador puede consultar todas las citas por fecha");
        }

        return citaRepository.findByFechaHoraBetween(inicio, fin);
    }
}
