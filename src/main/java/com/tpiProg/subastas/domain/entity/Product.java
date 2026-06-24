package com.tpiProg.subastas.domain.entity;

import jakarta.persistence.*;
import lombok.*;

// Tabla de los productos que se publicarán en las subastas
@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación de base de datos: Muchos a Uno. Muchos productos pertenecen a un Vendedor (Usuario)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    // Muchos productos pertenecen a una Categoría
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;
}