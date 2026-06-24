package com.tpiProg.subastas.domain.entity;

import com.tpiProg.subastas.domain.enums.RoleType;
import jakarta.persistence.*;
import lombok.*;

// @Entity le avisa a Spring que esto es una tabla de la base de datos
@Entity
@Table(name = "roles") // Nombre real que tendrá la tabla en Postgres
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    // @Id indica que esta es la Clave Primaria (Primary Key)
    // @GeneratedValue hace que el ID sea autoincremental (1, 2, 3...)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Aquí guardamos el ENUM que creaste en el paso anterior (USER, SELLER, ADMIN)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private RoleType name;
}