package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.ExpedienteMedico;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.ExpedienteMedicoRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.security.AutorizacionService;
import com.petsalud.backendpetsalud.service.ExpedienteMedicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ExpedienteMedicoServiceImpl implements ExpedienteMedicoService {

    @Autowired
    private ExpedienteMedicoRepository expedienteMedicoRepository;

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AutorizacionService autorizacionService;

    @Override
    public ExpedienteMedico agregarRegistro(Long mascotaId, Long veterinarioId, ExpedienteMedico registro) {
        autorizacionService.verificarIdentidadVeterinario(veterinarioId);

        // Semana 6: el registro debe traer descripcion, diagnostico y tratamiento
        validarRegistroCompleto(registro);

        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));

        Usuario veterinario = usuarioRepository.findById(veterinarioId)
                .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));

        // Semana 6: quien firma el registro debe ser realmente un veterinario
        if (veterinario.getRol() != Rol.VETERINARIO) {
            throw new IllegalArgumentException("El usuario indicado no es un veterinario");
        }

        registro.setMascota(mascota);
        registro.setVeterinario(veterinario);
        registro.setFecha(LocalDateTime.now());

        return expedienteMedicoRepository.save(registro);
    }

    @Override
    public Optional<ExpedienteMedico> obtenerRegistroPorId(Long id) {
        return expedienteMedicoRepository.findById(id);
    }

    @Override
    public List<ExpedienteMedico> listarRegistrosPorMascota(Long mascotaId) {
        return expedienteMedicoRepository.findByMascotaId(mascotaId);
    }

    @Override
    public void eliminarRegistro(Long id) {
        expedienteMedicoRepository.deleteById(id);
    }

    private void validarRegistroCompleto(ExpedienteMedico registro) {
        List<String> faltantes = new ArrayList<>();
        if (registro == null || estaVacio(registro.getDescripcion())) faltantes.add("descripcion");
        if (registro == null || estaVacio(registro.getDiagnostico())) faltantes.add("diagnostico");
        if (registro == null || estaVacio(registro.getTratamiento())) faltantes.add("tratamiento");
        if (!faltantes.isEmpty()) {
            throw new IllegalArgumentException(
                    "El registro medico esta incompleto, falta: " + String.join(", ", faltantes));
        }
    }

    private boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
