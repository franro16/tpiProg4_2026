package com.tpiProg.subastas.dto.request;

import com.tpiProg.subastas.domain.enums.AuctionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DisputeResolutionRequest(
        @NotBlank(message = "La resolución es obligatoria")
        String adminResolution,

        @NotNull(message = "El nuevo estado de la subasta es obligatorio (ADJUDICADA, FINALIZADA o CANCELADA)")
        AuctionStatus newAuctionStatus
) {}