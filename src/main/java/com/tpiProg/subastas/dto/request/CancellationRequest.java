package com.tpiProg.subastas.dto.request;

import jakarta.validation.constraints.NotBlank;

// Motivo obligatorio al cancelar una subasta
public record CancellationRequest(
        @NotBlank(message = "El motivo de cancelacion es obligatorio")
        String reason
) {}