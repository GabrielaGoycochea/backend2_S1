package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MascotaServiceImpl implements MascotaService {

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AutorizacionService autorizacionService;

    @Override
    public Mascota registrarMascota(Mascota mascota, Long propietarioId) {
        autorizacionService.verificarPersonalOPropietario(propietarioId);

        Usuario propietario = usuarioRepository.findById(propietarioId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Propietario no encontrado"));

        mascota.setPropietario(propietario);
        return mascotaRepository.save(mascota);
    }

    @Override
    public Optional<Mascota> obtenerMascotaPorId(Long id) {
        Optional<Mascota> mascota = mascotaRepository.findById(id);

        mascota.ifPresent(m ->
                autorizacionService.verificarPersonalOPropietario(propietarioIdDe(m)));

        return mascota;
    }

    @Override
    public Mascota actualizarMascota(Long id, Mascota mascotaActualizada) {
        return mascotaRepository.findById(id).map(mascota -> {
            autorizacionService.verificarPropietarioOAdmin(propietarioIdDe(mascota));

            mascota.setNombre(mascotaActualizada.getNombre());
            mascota.setEspecie(mascotaActualizada.getEspecie());
            mascota.setRaza(mascotaActualizada.getRaza());
            mascota.setFechaNacimiento(mascotaActualizada.getFechaNacimiento());
            mascota.setSexo(mascotaActualizada.getSexo());

            return mascotaRepository.save(mascota);
        }).orElseThrow(() ->
                new IllegalArgumentException("Mascota no encontrada"));
    }

    @Override
    public void eliminarMascota(Long id) {
        Mascota mascota = mascotaRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Mascota no encontrada"));

        autorizacionService.verificarPropietarioOAdmin(propietarioIdDe(mascota));

        mascotaRepository.delete(mascota);
    }

    @Override
    public List<Mascota> listarMascotasPorPropietario(Long propietarioId) {
        autorizacionService.verificarPersonalOPropietario(propietarioId);
        return mascotaRepository.findByPropietarioId(propietarioId);
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
