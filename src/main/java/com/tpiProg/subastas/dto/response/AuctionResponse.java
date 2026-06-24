package com.tpiProg.subastas.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// Lo que le mostramos a los usuarios cuando miran una subasta
public record AuctionResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal basePrice,
        BigDecimal currentPrice,
        BigDecimal minimumIncrement,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        String status,
        String description
) {}