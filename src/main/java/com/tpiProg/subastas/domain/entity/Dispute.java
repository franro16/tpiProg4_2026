package com.tpiProg.subastas.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

// Tabla para manejar los Reclamos o Disputas entre comprador y vendedor
@Entity
@Table(name = "disputes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    // Quién inició el reclamo (el ganador o el vendedor)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    // El Administrador que tomó el caso para resolverlo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_resolver_id")
    private User adminResolver;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "creation_date", nullable = false)
    private OffsetDateTime creationDate;

    // El veredicto final escrito por el administrador
    @Column(name = "admin_resolution", length = 2000)
    private String adminResolution;
}