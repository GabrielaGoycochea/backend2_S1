package com.petsalud.backendpetsalud.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "mascotas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String especie;

    private String raza;

    private LocalDate fechaNacimiento;

    private String sexo;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    @JsonIgnoreProperties({"mascotas", "ventas", "recompensas", "notificaciones"})
    private Usuario propietario;

    @JsonIgnore // evita el ciclo Mascota-Cita-Mascota
    @ToString.Exclude // S5: evita StackOverflowError en toString por la relación bidireccional
    @EqualsAndHashCode.Exclude // S5: idem en equals y hashCode
    @OneToMany(mappedBy = "mascota", cascade = CascadeType.ALL)
    private List<Cita> citas;

    @JsonIgnore // evita el ciclo Mascota-Cita-Mascota
    @ToString.Exclude // S5: evita StackOverflowError en toString por la relación bidireccional
    @EqualsAndHashCode.Exclude // S5: idem en equals y hashCode
    @OneToMany(mappedBy = "mascota", cascade = CascadeType.ALL)
    private List<ExpedienteMedico> expedientesMedicos;
}
