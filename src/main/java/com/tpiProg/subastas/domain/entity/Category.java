package com.tpiProg.subastas.domain.entity;

import jakarta.persistence.*;
import lombok.*;

// Tabla que guarda las categorías (ej: Electrónica, Muebles, etc.)
@Entity
@Table(name = "categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // A la descripción le damos un límite de 500 caracteres
    @Column(length = 500)
    private String description;
}