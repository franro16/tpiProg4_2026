package com.tpiProg.subastas.domain.entity;

import com.tpiProg.subastas.domain.enums.AuctionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

// Tabla de auditoría: guarda CADA VEZ que una subasta cambia de estado para que no haya trampas
@Entity
@Table(name = "auction_state_histories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionStateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    // El usuario (o el sistema) que provocó el cambio de estado
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsible_user_id", nullable = false)
    private User responsibleUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_state", nullable = false, length = 30)
    private AuctionStatus previousState;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_state", nullable = false, length = 30)
    private AuctionStatus newState;

    @Column(name = "change_date", nullable = false)
    private OffsetDateTime changeDate;

    // Por qué se cambió de estado (Ej: "Cierre automático sin pujas")
    @Column(nullable = false, length = 500)
    private String reason;
}