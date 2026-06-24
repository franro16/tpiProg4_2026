package com.tpiProg.subastas.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// Respuesta con los datos de una puja realizada
public record BidResponse(
        Long id,
        Long auctionId,
        Long userId,
        String username,
        BigDecimal amount,
        OffsetDateTime bidDate
) {}