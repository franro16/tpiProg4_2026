package com.tpiProg.subastas.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

// Tabla para las Notificaciones (la campanita de avisos para el usuario)
@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A quién le llega el aviso
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "creation_date", nullable = false)
    private OffsetDateTime creationDate;

    // Para saber si el usuario ya vio el mensaje o no (true/false)
    @Column(name = "is_read", nullable = false)
    private boolean isRead;
}