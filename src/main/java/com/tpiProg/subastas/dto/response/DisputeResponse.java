package com.tpiProg.subastas.dto.response;

import java.time.OffsetDateTime;

// Lo que ve el administrador cuando revisa las disputas
public record DisputeResponse(
        Long id,
        Long auctionId,
        String initiatorUsername,
        String reason,
        String description,
        OffsetDateTime creationDate,
        String adminResolution
) {}