package com.tpiProg.subastas.dto.response;

import java.time.OffsetDateTime;

// Un registro del historial de cambios de estado de una subasta
public record AuctionStateHistoryResponse(
        Long id,
        String previousState,
        String newState,
        OffsetDateTime changeDate,
        String reason,
        String responsibleUsername
) {}