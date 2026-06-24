package com.tpiProg.subastas.domain.entity;

import com.tpiProg.subastas.domain.enums.AuctionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// Esta es la tabla principal: La Subasta
@Entity
@Table(name = "auctions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación: Una subasta pertenece a un Producto específico
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Relación: El usuario que termina ganando la subasta (puede ser nulo al principio)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    // precision = 19, scale = 4 significa que el número puede tener 19 dígitos en total,
    // y 4 de ellos son decimales. Ideal para plata exacta.
    @Column(name = "base_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal basePrice;

    @Column(name = "minimum_increment", nullable = false, precision = 19, scale = 4)
    private BigDecimal minimumIncrement;

    @Column(name = "current_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPrice;

    // Usamos OffsetDateTime para guardar las fechas siempre en formato UTC
    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private OffsetDateTime endDate;

    @Column(name = "adjudication_date")
    private OffsetDateTime adjudicationDate;

    // Guardamos el estado actual (ACTIVA, FINALIZADA, etc.) usando el Enum que creamos
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuctionStatus status;

    @Column(length = 2000)
    private String description;

    // Este campo nos ayuda a evitar que dos usuarios modifiquen la subasta al mismo tiempo
    @Version
    private Long version;

    // --- MÉTODOS DE NEGOCIO (Comportamientos de la subasta) ---

    // Verifica si la fecha actual está dentro del tiempo de la subasta
    public boolean validarPeriodoSubasta(OffsetDateTime now) {
        return now.isAfter(startDate) && now.isBefore(endDate);
    }

    public void cambiarEstado(AuctionStatus nuevoEstado) {
        this.status = nuevoEstado;
    }

    // Solo se puede abrir un reclamo si la subasta ya tiene un ganador (ADJUDICADA)
    public boolean aptaParaDisputa() {
        return this.status == AuctionStatus.ADJUDICADA;
    }

    // Comprueba si ya se pasó la fecha de cierre
    public boolean evaluarCierreAutomatico(OffsetDateTime now) {
        return now.isAfter(endDate) || now.isEqual(endDate);
    }
}