package com.tpiProg.subastas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Datos para abrir un reclamo/disputa
public record DisputeRequest(
        @NotNull(message = "El ID de la subasta es obligatorio")
        Long auctionId,

        @NotBlank(message = "El motivo es obligatorio")
        String reason,

        @NotBlank(message = "La descripción es obligatoria")
        String description
) {}