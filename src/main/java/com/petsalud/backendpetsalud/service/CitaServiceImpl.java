package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.EstadoCita;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.CitaRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CitaServiceImpl implements CitaService {

    private static final EstadoCita AGENDADA = EstadoCita.AGENDADA;
    private static final EstadoCita CANCELADA = EstadoCita.CANCELADA;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AutorizacionService autorizacionService;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Cita agendarCita(Long mascotaId, Long veterinarioId, LocalDateTime fechaHora, String motivo) {
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));

        // S5 La mascota debe tener un propietario registrado
        autorizacionService.verificarPropietarioOAdmin(propietarioIdDe(mascota));

        Usuario veterinario = usuarioRepository.buscarPorIdConBloqueo(veterinarioId)
                .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));

        // S5 El usuario asignado debe ser realmente un veterinario
        if (veterinario.getRol() != Rol.VETERINARIO) {
            throw new IllegalArgumentException("El usuario indicado no es un veterinario");
        }

        // S5 No se puede agendar en un horario ya ocupado por el veterinario
        if (citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNot(veterinarioId, fechaHora, CANCELADA)) {
            throw new IllegalArgumentException("El veterinario ya tiene una cita agendada en ese horario");
        }

        Cita cita = Cita.builder()
                .mascota(mascota)
                .veterinario(veterinario)
                .fechaHora(fechaHora)
                .motivo(motivo)
                .estado(AGENDADA)
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

        boolean esAdmin = usuario.getRol() == Rol.ADMIN;

        boolean esPropietario =
                usuario.getRol() == Rol.CLIENTE
                && cita.getMascota() != null
                && cita.getMascota().getPropietario() != null
                && usuario.getId().equals(cita.getMascota().getPropietario().getId());

        boolean esVeterinarioAsignado =
                usuario.getRol() == Rol.VETERINARIO
                && cita.getVeterinario() != null
                && usuario.getId().equals(cita.getVeterinario().getId());

        if (!esAdmin && !esPropietario && !esVeterinarioAsignado) {
            throw new AccessDeniedException("No tiene permiso para consultar esta cita");
        }

        return resultado;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Cita actualizarEstadoCita(Long id, String nuevoEstado) {
        Cita cita = citaRepository.buscarPorIdConBloqueo(id)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));

        Usuario usuario = autorizacionService.obtenerUsuarioAutenticado();

        boolean esAdmin = usuario.getRol() == Rol.ADMIN;

        boolean esVeterinarioAsignado =
                usuario.getRol() == Rol.VETERINARIO
                && cita.getVeterinario() != null
                && usuario.getId().equals(cita.getVeterinario().getId());

        if (!esAdmin && !esVeterinarioAsignado) {
            throw new AccessDeniedException("No tiene permiso para modificar esta cita");
        }

        EstadoCita destino = EstadoCita.desdeTexto(nuevoEstado);
        if ((cita.getEstado() == CANCELADA || cita.getEstado() == null) && destino != CANCELADA) {
            if (cita.getVeterinario() == null) {
                throw new IllegalArgumentException("La cita no tiene veterinario asignado");
            }
            Long veterinarioId = cita.getVeterinario().getId();
            usuarioRepository.buscarPorIdConBloqueo(veterinarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));
            if (citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNot(veterinarioId, cita.getFechaHora(), CANCELADA)) {
                throw new IllegalArgumentException("El veterinario ya tiene una cita agendada en ese horario");
            }
        }
        cita.setEstado(destino);
        return citaRepository.save(cita);
    }

    @Override
    @Transactional
    public void cancelarCita(Long id) {
        Cita cita = citaRepository.buscarPorIdConBloqueo(id)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));

        if (cita.getMascota() == null) {
            throw new IllegalArgumentException("La cita no tiene una mascota asociada");
        }
        autorizacionService.verificarPropietarioOAdmin(propietarioIdDe(cita.getMascota()));

        cita.setEstado(CANCELADA);
        citaRepository.save(cita);
    }

    @Override
    public List<Cita> listarCitasPorMascota(Long mascotaId) {
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));

        Usuario usuario = autorizacionService.obtenerUsuarioAutenticado();

        if (usuario.getRol() == Rol.ADMIN) {
            return citaRepository.findByMascotaId(mascotaId);
        }

        if (usuario.getRol() == Rol.CLIENTE) {
            autorizacionService.verificarPropietarioOAdmin(propietarioIdDe(mascota));
            return citaRepository.findByMascotaId(mascotaId);
        }

        if (usuario.getRol() == Rol.VETERINARIO) {
            return citaRepository.findByMascotaId(mascotaId).stream()
                    .filter(cita -> cita.getVeterinario() != null
                            && usuario.getId().equals(cita.getVeterinario().getId()))
                    .toList();
        }

        throw new AccessDeniedException("No tiene permiso para consultar estas citas");
    }

    @Override
    public List<Cita> listarCitasPorVeterinario(Long veterinarioId) {
        Usuario usuario = autorizacionService.obtenerUsuarioAutenticado();

        if (usuario.getRol() != Rol.ADMIN
                && (usuario.getRol() != Rol.VETERINARIO || !usuario.getId().equals(veterinarioId))) {
            throw new AccessDeniedException("No tiene permiso para consultar estas citas");
        }

        return citaRepository.findByVeterinarioId(veterinarioId);
    }

    @Override
    public List<Cita> listarCitasEntreFechas(LocalDateTime inicio, LocalDateTime fin) {
        Usuario usuario = autorizacionService.obtenerUsuarioAutenticado();

        if (usuario.getRol() != Rol.ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede consultar todas las citas por fecha");
        }

        return citaRepository.findByFechaHoraBetween(inicio, fin);
    }

    // S5 Evita un NullPointerException (error 500) cuando la mascota no tiene propietario registrado
    private Long propietarioIdDe(Mascota mascota) {
        Usuario propietario = mascota.getPropietario();
        if (propietario == null || propietario.getId() == null) {
            throw new IllegalArgumentException("La mascota no tiene un propietario registrado");
        }
        return propietario.getId();
    }
}
