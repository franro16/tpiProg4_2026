package com.tpiProg.subastas.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

// Lo que manda el usuario cuando aprieta el botón de "Pujar"
public record BidRequest(
        @NotNull(message = "El ID de la subasta es obligatorio")
        Long auctionId,

        @NotNull(message = "El monto de la puja es obligatorio")
        @Positive(message = "El monto de la puja debe ser mayor a cero")
        BigDecimal amount
) {}