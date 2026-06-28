package com.tpiProg.subastas.controller;

import com.tpiProg.subastas.dto.request.BidRequest;
import com.tpiProg.subastas.dto.response.BidResponse;
import com.tpiProg.subastas.service.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auctions/{auctionId}/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    // Solo los perfiles de tipo USER pueden efectuar pujas. 
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BidResponse> placeBid(
            @PathVariable Long auctionId,
            @Valid @RequestBody BidRequest request,
            Authentication authentication) {
        
        // Notar que pisamos el auctionId del RequestBody con el del PathVariable por seguridad
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bidService.placeBid(auctionId, request, authentication.getName()));
    }

    // Un usuario registrado puede ver su propio historial en una subasta específica
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BidResponse>> getMyBids(
            @PathVariable Long auctionId,
            Authentication authentication) {
        return ResponseEntity.ok(bidService.getMyBids(auctionId, authentication.getName()));
    }

    // El ADMIN o el SELLER (al finalizar) pueden ver el historial completo
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<List<BidResponse>> getAuctionBids(
            @PathVariable Long auctionId,
            Authentication authentication) {
        return ResponseEntity.ok(bidService.getAuctionBids(auctionId, authentication));
    }
}