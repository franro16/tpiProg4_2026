package com.tpiProg.subastas.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// Esta tabla guarda las Pujas (las ofertas de dinero que hace la gente)
@Entity
@Table(name = "bids")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ¿A qué subasta pertenece esta puja?
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    // ¿Quién es el usuario que ofertó la plata?
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // El monto exacto ofertado
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    // Fecha y hora exacta de la oferta
    @Column(name = "bid_date", nullable = false)
    private OffsetDateTime bidDate;

    // --- MÉTODOS DE NEGOCIO ---

    // Valida que la primera oferta sea igual o mayor al precio base
    public boolean validarMontoMinimo(BigDecimal precioBase) {
        return this.amount.compareTo(precioBase) >= 0;
    }

    // Valida que las siguientes ofertas superen a la anterior por el incremento mínimo exigido
    public boolean validarIncrementoMinimo(BigDecimal incrementoMinimo, BigDecimal precioActual) {
        return this.amount.compareTo(precioActual.add(incrementoMinimo)) >= 0;
    }
}