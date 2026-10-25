package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Notificacion;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import com.petsalud.backendpetsalud.repository.NotificacionRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificacionServiceImpl implements NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AutorizacionService autorizacionService;

    @Override
    public Notificacion enviarNotificacion(Long usuarioId, Notificacion notificacion) {
        Usuario usuarioAutenticado = autorizacionService.obtenerUsuarioAutenticado();

        if (usuarioAutenticado.getRol() != com.petsalud.backendpetsalud.entity.Rol.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo un administrador puede enviar notificaciones");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        notificacion.setUsuario(usuario);
        notificacion.setFecha(LocalDateTime.now());
        notificacion.setLeida(false);

        return notificacionRepository.save(notificacion);
    }

    @Override
    public Optional<Notificacion> obtenerNotificacionPorId(Long id) {
        Optional<Notificacion> resultado = notificacionRepository.findById(id);

        resultado.ifPresent(notificacion ->
                autorizacionService.verificarPropietarioOAdmin(
                        notificacion.getUsuario().getId()
                )
        );

        return resultado;
    }

    @Override
    public List<Notificacion> listarNotificacionesPorUsuario(Long usuarioId) {
        autorizacionService.verificarPropietarioOAdmin(usuarioId);
        return notificacionRepository.findByUsuarioId(usuarioId);
    }

    @Override
    public List<Notificacion> listarNotificacionesNoLeidas(Long usuarioId) {
        autorizacionService.verificarPropietarioOAdmin(usuarioId);
        return notificacionRepository.findByLeidaFalseAndUsuarioId(usuarioId);
    }

    @Override
    public void marcarComoLeida(Long id) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada"));

        autorizacionService.verificarPropietarioOAdmin(
                notificacion.getUsuario().getId()
        );

        notificacion.setLeida(true);
        notificacionRepository.save(notificacion);
    }

    @Override
    public void eliminarNotificacion(Long id) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada"));

        autorizacionService.verificarPropietarioOAdmin(
                notificacion.getUsuario().getId()
        );

        notificacionRepository.deleteById(id);
    }
}
