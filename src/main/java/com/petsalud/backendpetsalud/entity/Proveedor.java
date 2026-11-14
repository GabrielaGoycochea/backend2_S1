package com.petsalud.backendpetsalud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "proveedores")
@Data // Lombok genera getter y setter automáticamente
@Builder // Lombok genera un patrón de diseño para construir objetos de la clase Proveedor
@NoArgsConstructor //Lombok genera un constructor vacio
@AllArgsConstructor // Lombok genera un constructor con todos los atributos
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;
}
