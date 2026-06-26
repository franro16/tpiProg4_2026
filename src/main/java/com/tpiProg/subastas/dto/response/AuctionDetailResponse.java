package com.tpiProg.subastas.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// Vista completa de una subasta (para ADMIN, vendedor tras finalizar, o el propio usuario)
public record AuctionDetailResponse(
        Long id,
        Long productId,
        String productName,
        String sellerUsername,
        BigDecimal basePrice,
        BigDecimal currentPrice,
        BigDecimal minimumIncrement,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        OffsetDateTime adjudicationDate,
        String status,
        String description,
        String winnerUsername
) {}