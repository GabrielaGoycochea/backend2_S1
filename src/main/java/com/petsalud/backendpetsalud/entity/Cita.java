package com.petsalud.backendpetsalud.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "citas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fechaHora;

    // Semana 6: enum en vez de texto libre (se guarda como AGENDADA, CANCELADA o COMPLETADA)
    @Enumerated(EnumType.STRING)
    private EstadoCita estado;

    private String motivo;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "mascota_id")
    private Mascota mascota;

    @ManyToOne
    @JoinColumn(name = "veterinario_id")
    private Usuario veterinario;

    /**
     * Semana 6: clave del horario que ocupa la cita (veterinario y fecha). Es unica en la base de datos,
     * de modo que dos citas activas no pueden compartir horario aunque las solicitudes lleguen a la vez.
     * Una cita cancelada la deja en null (los NULL no chocan entre si), por lo que libera el horario
     * sin borrar el registro.
     */
    @JsonIgnore
    @Column(name = "horario_activo", unique = true)
    private String horarioActivo;

    @PrePersist
    @PreUpdate
    private void actualizarHorarioActivo() {
        boolean ocupaHorario = estado != EstadoCita.CANCELADA && veterinario != null && fechaHora != null;
        horarioActivo = ocupaHorario ? veterinario.getId() + "|" + fechaHora : null;
    }
}
