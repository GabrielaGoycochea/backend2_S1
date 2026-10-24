package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Reglas de PROPIEDAD de los recursos, usadas desde @PreAuthorize con la expresión "@seg.xxx(...)".
 *
 * Evita el ataque IDOR (Insecure Direct Object Reference): que un cliente autenticado cambie
 * el id en la URL (/api/mascotas/15) y lea o modifique datos de otra persona.
 * Si el recurso no existe se responde false (403), sin revelar si el id existe o no.
 */
@Component("seg")
@Transactional(readOnly = true)
public class SeguridadService {

    private final MascotaRepository mascotaRepository;
    private final CitaRepository citaRepository;
    private final ExpedienteMedicoRepository expedienteMedicoRepository;
    private final VentaRepository ventaRepository;
    private final NotificacionRepository notificacionRepository;
    private final RecompensaRepository recompensaRepository;

    public SeguridadService(MascotaRepository mascotaRepository,
                            CitaRepository citaRepository,
                            ExpedienteMedicoRepository expedienteMedicoRepository,
                            VentaRepository ventaRepository,
                            NotificacionRepository notificacionRepository,
                            RecompensaRepository recompensaRepository) {
        this.mascotaRepository = mascotaRepository;
        this.citaRepository = citaRepository;
        this.expedienteMedicoRepository = expedienteMedicoRepository;
        this.ventaRepository = ventaRepository;
        this.notificacionRepository = notificacionRepository;
        this.recompensaRepository = recompensaRepository;
    }

    /** Id del usuario autenticado en la petición actual (null si es anónimo). */
    public Long idUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioDetails detalles) {
            return detalles.getId();
        }
        return null;
    }

    /** ¿El id recibido corresponde al usuario autenticado? */
    public boolean esUsuarioActual(Long usuarioId) {
        Long actual = idUsuarioActual();
        return actual != null && Objects.equals(actual, usuarioId);
    }

    public boolean esDuenoMascota(Long mascotaId) {
        return mascotaId != null && mascotaRepository.findById(mascotaId)
                .map(m -> m.getPropietario() != null && esUsuarioActual(m.getPropietario().getId()))
                .orElse(false);
    }

    public boolean esDuenoCita(Long citaId) {
        return citaId != null && citaRepository.findById(citaId)
                .map(c -> c.getMascota() != null && c.getMascota().getPropietario() != null
                        && esUsuarioActual(c.getMascota().getPropietario().getId()))
                .orElse(false);
    }

    public boolean esVeterinarioDeCita(Long citaId) {
        return citaId != null && citaRepository.findById(citaId)
                .map(Cita::getVeterinario)
                .map(v -> esUsuarioActual(v.getId()))
                .orElse(false);
    }

    /** El tutor de la mascota o el veterinario asignado pueden ver la cita. */
    public boolean puedeVerCita(Long citaId) {
        return esDuenoCita(citaId) || esVeterinarioDeCita(citaId);
    }

    public boolean esDuenoExpediente(Long expedienteId) {
        return expedienteId != null && expedienteMedicoRepository.findById(expedienteId)
                .map(e -> e.getMascota() != null && e.getMascota().getPropietario() != null
                        && esUsuarioActual(e.getMascota().getPropietario().getId()))
                .orElse(false);
    }

    public boolean esDuenoVenta(Long ventaId) {
        return ventaId != null && ventaRepository.findById(ventaId)
                .map(v -> v.getUsuario() != null && esUsuarioActual(v.getUsuario().getId()))
                .orElse(false);
    }

    public boolean esDuenoNotificacion(Long notificacionId) {
        return notificacionId != null && notificacionRepository.findById(notificacionId)
                .map(n -> n.getUsuario() != null && esUsuarioActual(n.getUsuario().getId()))
                .orElse(false);
    }

    public boolean esDuenoRecompensa(Long recompensaId) {
        return recompensaId != null && recompensaRepository.findById(recompensaId)
                .map(r -> r.getUsuario() != null && esUsuarioActual(r.getUsuario().getId()))
                .orElse(false);
    }
}
