package com.tpiProg.subastas.controller;

import com.tpiProg.subastas.dto.request.AuctionRequest;
import com.tpiProg.subastas.dto.request.CancellationRequest;
import com.tpiProg.subastas.dto.response.AuctionDetailResponse;
import com.tpiProg.subastas.dto.response.AuctionResponse;
import com.tpiProg.subastas.dto.response.AuctionStateHistoryResponse;
import com.tpiProg.subastas.security.UserPrincipal;
import com.tpiProg.subastas.service.AuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    // Cualquiera puede listar subastas (publico)
    @GetMapping
    public ResponseEntity<List<AuctionResponse>> getAll() {
        return ResponseEntity.ok(auctionService.getAll());
    }

    // Detalle: publico, pero la visibilidad del ganador depende del usuario logueado
    @GetMapping("/{id}")
    public ResponseEntity<AuctionDetailResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(auctionService.getById(id, userId));
    }

    // Solo SELLER puede crear una subasta
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<AuctionResponse> create(
            @Valid @RequestBody AuctionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auctionService.create(request, principal.getId()));
    }

    // Solo SELLER puede publicar su propia subasta
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<AuctionResponse> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(auctionService.publish(id, principal.getId()));
    }

    // Cancelar: SELLER (sin pujas) o ADMIN (con o sin pujas)
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancellationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        auctionService.cancel(id, principal.getId(), isAdmin, request);
        return ResponseEntity.noContent().build();
    }

    // Historial: solo ADMIN ve cualquier historial
    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuctionStateHistoryResponse>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getHistory(id));
    }
}