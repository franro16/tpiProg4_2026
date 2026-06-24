package com.tpiProg.subastas.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

// Usamos "app_users" porque la palabra "user" suele estar prohibida o reservada en SQL
@Entity
@Table(name = "app_users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // nullable = false significa que es obligatorio (no puede ser nulo)
    // unique = true significa que no puede haber dos usuarios con el mismo nombre
    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    // name = "password_hash" le cambia el nombre a la columna en la base de datos
    @Column(nullable = false, name = "password_hash")
    private String passwordHash;

    @Column(nullable = false, name = "is_blocked")
    private boolean isBlocked;

    // Relación de base de datos: Muchos a Muchos. Un usuario puede tener varios roles
    // y un rol lo pueden tener muchos usuarios. Crea una tabla intermedia llamada "user_roles".
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
}