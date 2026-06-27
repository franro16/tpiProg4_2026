package com.tpiProg.subastas.service.impl;

import com.tpiProg.subastas.domain.entity.Auction;
import com.tpiProg.subastas.domain.entity.AuctionStateHistory;
import com.tpiProg.subastas.domain.entity.Dispute;
import com.tpiProg.subastas.domain.entity.User;
import com.tpiProg.subastas.domain.enums.AuctionStatus;
import com.tpiProg.subastas.dto.request.DisputeRequest;
import com.tpiProg.subastas.dto.request.DisputeResolutionRequest;
import com.tpiProg.subastas.dto.response.DisputeResponse;
import com.tpiProg.subastas.exception.AuctionStateException;
import com.tpiProg.subastas.exception.BusinessException;
import com.tpiProg.subastas.exception.ResourceNotFoundException;
import com.tpiProg.subastas.exception.UnauthorizedException;
import com.tpiProg.subastas.repository.AuctionRepository;
import com.tpiProg.subastas.repository.AuctionStateHistoryRepository;
import com.tpiProg.subastas.repository.DisputeRepository;
import com.tpiProg.subastas.repository.UserRepository;
import com.tpiProg.subastas.service.DisputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeServiceImpl implements DisputeService {

    private final DisputeRepository disputeRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final AuctionStateHistoryRepository historyRepository;

    @Override
    @Transactional
    public DisputeResponse openDispute(DisputeRequest request, String username) {
        User initiator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", 0L));

        Auction auction = auctionRepository.findById(request.auctionId())
                .orElseThrow(() -> new ResourceNotFoundException("Subasta", request.auctionId()));

        if (auction.getStatus() != AuctionStatus.ADJUDICADA) {
            throw new AuctionStateException("Solo se pueden abrir disputas en subastas en estado ADJUDICADA.");
        }

        boolean isSeller = auction.getProduct().getSeller().getId().equals(initiator.getId());
        boolean isWinner = auction.getWinner() != null && auction.getWinner().getId().equals(initiator.getId());

        if (!isSeller && !isWinner) {
            throw new UnauthorizedException("Solo el vendedor o el ganador pueden abrir una disputa para esta subasta.");
        }

        Dispute dispute = Dispute.builder()
                .auction(auction)
                .initiator(initiator)
                .reason(request.reason())
                .description(request.description())
                .creationDate(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        disputeRepository.save(dispute);

        AuctionStatus previousStatus = auction.getStatus();
        auction.setStatus(AuctionStatus.EN_DISPUTA);
        auctionRepository.save(auction);

        registrarHistorial(auction, initiator, previousStatus, AuctionStatus.EN_DISPUTA, "Disputa iniciada: " + request.reason());

        log.info("Disputa abierta en subasta {} por el usuario {}", auction.getId(), username);
        return toResponse(dispute);
    }

    @Override
    @Transactional
    public DisputeResponse resolveDispute(Long disputeId, DisputeResolutionRequest request, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", 0L));

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Disputa", disputeId));

        if (dispute.getAdminResolution() != null) {
            throw new BusinessException("Esta disputa ya ha sido resuelta previamente.");
        }

        if (request.newAuctionStatus() != AuctionStatus.ADJUDICADA &&
            request.newAuctionStatus() != AuctionStatus.FINALIZADA &&
            request.newAuctionStatus() != AuctionStatus.CANCELADA) {
            throw new BusinessException("El estado de resolución es inválido. Debe ser ADJUDICADA, FINALIZADA o CANCELADA.");
        }

        Auction auction = dispute.getAuction();
        
        dispute.setAdminResolver(admin);
        dispute.setAdminResolution(request.adminResolution());
        disputeRepository.save(dispute);

        AuctionStatus previousStatus = auction.getStatus();
        auction.setStatus(request.newAuctionStatus());
        auctionRepository.save(auction);

        registrarHistorial(auction, admin, previousStatus, request.newAuctionStatus(), "Resolución de disputa de admin: " + request.adminResolution());

        log.info("Disputa {} resuelta por admin {}. Nuevo estado de subasta: {}", disputeId, adminUsername, request.newAuctionStatus());
        return toResponse(dispute);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputeResponse> getAllDisputes() {
        return disputeRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputeResponse> getMyDisputes(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", 0L));
        
        return disputeRepository.findAll().stream()
                .filter(d -> d.getInitiator().getId().equals(user.getId()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void registrarHistorial(Auction auction, User user, AuctionStatus previous, AuctionStatus current, String reason) {
        AuctionStateHistory history = AuctionStateHistory.builder()
                .auction(auction)
                .responsibleUser(user)
                // Aca está la corrección clave: Pasamos el Enum puro, no el .name()
                .previousState(previous)
                .newState(current)
                .changeDate(OffsetDateTime.now(ZoneOffset.UTC))
                .reason(reason)
                .build();
        historyRepository.save(history);
    }

    private DisputeResponse toResponse(Dispute dispute) {
        String adminResolution = dispute.getAdminResolution() != null ? dispute.getAdminResolution() : "Pendiente de resolución";
        
        return new DisputeResponse(
                dispute.getId(),
                dispute.getAuction().getId(),
                dispute.getInitiator().getUsername(),
                dispute.getReason(),
                dispute.getDescription(),
                dispute.getCreationDate(),
                adminResolution
        );
    }
}