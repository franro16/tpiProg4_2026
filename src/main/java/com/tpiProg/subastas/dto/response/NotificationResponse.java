package com.tpiProg.subastas.dto.response;

import java.time.OffsetDateTime;

public record NotificationResponse(
        Long id,
        String message,
        OffsetDateTime creationDate,
        boolean isRead
) {}