package com.petsalud.backendpetsalud.repository;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {
    List<Cita> findByMascotaId(Long mascotaId);
    List<Cita> findByVeterinarioId(Long veterinarioId);
    List<Cita> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);

    // Semana 5: horario ocupado = el veterinario ya tiene una cita en esa fecha y hora que no esta cancelada
    boolean existsByVeterinarioIdAndFechaHoraAndEstadoNot(Long veterinarioId, LocalDateTime fechaHora, EstadoCita estado);
}
