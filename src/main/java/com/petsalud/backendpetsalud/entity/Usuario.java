package com.petsalud.backendpetsalud.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Entity
@Table(name = "usuarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // nunca se devuelve en las respuestas
    @ToString.Exclude // S5: el hash tampoco debe quedar en los logs si alguien imprime el usuario
    @Column(nullable = false)
    private String password;

    private String nombre;

    private String apellido;

    private String telefono;

    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    // Relaciones
    @JsonIgnore // evita ciclos Usuario-Venta-Usuario y no expone datos de otras tablas
    @ToString.Exclude // S5: evita StackOverflowError en toString por la relación bidireccional
    @EqualsAndHashCode.Exclude // S5: idem en equals y hashCode
    @OneToMany(mappedBy = "propietario", cascade = CascadeType.ALL)
    private List<Mascota> mascotas;

    @JsonIgnore // evita ciclos Usuario-Venta-Usuario y no expone datos de otras tablas
    @ToString.Exclude // S5: evita StackOverflowError en toString por la relación bidireccional
    @EqualsAndHashCode.Exclude // S5: idem en equals y hashCode
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Venta> ventas;

    @JsonIgnore // evita ciclos Usuario-Venta-Usuario y no expone datos de otras tablas
    @ToString.Exclude // S5: evita StackOverflowError en toString por la relación bidireccional
    @EqualsAndHashCode.Exclude // S5: idem en equals y hashCode
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Recompensa> recompensas;

    @JsonIgnore // evita ciclos Usuario-Venta-Usuario y no expone datos de otras tablas
    @ToString.Exclude // S5: evita StackOverflowError en toString por la relación bidireccional
    @EqualsAndHashCode.Exclude // S5: idem en equals y hashCode
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Notificacion> notificaciones;
}