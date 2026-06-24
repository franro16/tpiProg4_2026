package com.tpiProg.subastas.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

// Datos que manda el vendedor para crear una subasta
public record AuctionRequest(
        @NotNull(message = "El ID del producto es obligatorio")
        Long productId,

        @NotNull(message = "El precio base es obligatorio")
        @PositiveOrZero(message = "El precio base no puede ser negativo")
        BigDecimal basePrice,

        @NotNull(message = "El incremento mínimo es obligatorio")
        @Positive(message = "El incremento mínimo debe ser mayor a cero")
        BigDecimal minimumIncrement,

        @NotNull(message = "La fecha de inicio es obligatoria")
        @FutureOrPresent(message = "La fecha de inicio no puede estar en el pasado")
        OffsetDateTime startDate,

        @NotNull(message = "La fecha de cierre es obligatoria")
        @Future(message = "La fecha de cierre debe ser en el futuro")
        OffsetDateTime endDate,

        String description
) {}