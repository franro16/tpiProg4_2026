package com.tpiProg.subastas.controller;

import com.tpiProg.subastas.dto.request.DisputeRequest;
import com.tpiProg.subastas.dto.request.DisputeResolutionRequest;
import com.tpiProg.subastas.dto.response.DisputeResponse;
import com.tpiProg.subastas.service.DisputeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping("/auctions/{auctionId}/disputes")
    @PreAuthorize("hasAnyRole('USER', 'SELLER')")
    public ResponseEntity<DisputeResponse> openDispute(
            @PathVariable Long auctionId,
            @Valid @RequestBody DisputeRequest request,
            Authentication authentication) {
        
        // Reconstruimos el request para garantizar que se usa el auctionId de la URL por seguridad
        DisputeRequest securedRequest = new DisputeRequest(auctionId, request.reason(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disputeService.openDispute(securedRequest, authentication.getName()));
    }

    @PatchMapping("/disputes/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @PathVariable Long id,
            @Valid @RequestBody DisputeResolutionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(disputeService.resolveDispute(id, request, authentication.getName()));
    }

    @GetMapping("/disputes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DisputeResponse>> getAllDisputes() {
        return ResponseEntity.ok(disputeService.getAllDisputes());
    }

    @GetMapping("/disputes/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DisputeResponse>> getMyDisputes(Authentication authentication) {
        return ResponseEntity.ok(disputeService.getMyDisputes(authentication.getName()));
    }
}