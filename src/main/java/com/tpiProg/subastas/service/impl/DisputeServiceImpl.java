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
    public DisputeResponse openDispute(DisputeRequest request, String userEmail) {
        // authentication.getName() devuelve email
        User initiator = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado", 0L));

        Auction auction = auctionRepository.findById(request.auctionId())
                .orElseThrow(() -> new ResourceNotFoundException("Subasta", request.auctionId()));

        if (auction.getStatus() != AuctionStatus.ADJUDICADA) {
            throw new AuctionStateException(
                    "Solo se pueden abrir disputas en subastas en estado ADJUDICADA.");
        }

        boolean isSeller = auction.getProduct().getSeller().getId().equals(initiator.getId());
        boolean isWinner = auction.getWinner() != null
                && auction.getWinner().getId().equals(initiator.getId());

        if (!isSeller && !isWinner) {
            throw new UnauthorizedException(
                    "Solo el vendedor o el ganador pueden abrir una disputa.");
        }

        Dispute dispute = Dispute.builder()
                .auction(auction)
                .initiator(initiator)
                .reason(request.reason())
                .description(request.description())
                .creationDate(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        disputeRepository.save(dispute);

        AuctionStatus estadoAnterior = auction.getStatus();
        auction.setStatus(AuctionStatus.EN_DISPUTA);
        auctionRepository.save(auction);

        registrarHistorial(auction, initiator, estadoAnterior, AuctionStatus.EN_DISPUTA,
                "Disputa iniciada: " + request.reason());

        log.info("Disputa abierta en subasta={} por usuario={}", auction.getId(), userEmail);
        return toResponse(dispute);
    }

    @Override
    @Transactional
    public DisputeResponse resolveDispute(Long disputeId, DisputeResolutionRequest request,
                                          String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado", 0L));

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Disputa", disputeId));

        if (dispute.getAdminResolution() != null) {
            throw new BusinessException("Esta disputa ya fue resuelta.");
        }

        if (request.newAuctionStatus() != AuctionStatus.ADJUDICADA
                && request.newAuctionStatus() != AuctionStatus.FINALIZADA
                && request.newAuctionStatus() != AuctionStatus.CANCELADA) {
            throw new BusinessException(
                    "Estado de resolucion invalido. Valores permitidos: ADJUDICADA, FINALIZADA, CANCELADA.");
        }

        Auction auction = dispute.getAuction();

        dispute.setAdminResolver(admin);
        dispute.setAdminResolution(request.adminResolution());
        disputeRepository.save(dispute);

        AuctionStatus estadoAnterior = auction.getStatus();
        auction.setStatus(request.newAuctionStatus());
        auctionRepository.save(auction);

        registrarHistorial(auction, admin, estadoAnterior, request.newAuctionStatus(),
                "Resolucion de disputa por admin: " + request.adminResolution());

        log.info("Disputa={} resuelta por admin={}. Nuevo estado subasta={}",
                disputeId, adminEmail, request.newAuctionStatus());
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
    public List<DisputeResponse> getMyDisputes(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado", 0L));

        return disputeRepository.findAll().stream()
                .filter(d -> d.getInitiator().getId().equals(user.getId()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void registrarHistorial(Auction auction, User responsable,
                                    AuctionStatus anterior, AuctionStatus nuevo, String motivo) {
        AuctionStateHistory history = AuctionStateHistory.builder()
                .auction(auction)
                .responsibleUser(responsable)
                .previousState(anterior)
                .newState(nuevo)
                .changeDate(OffsetDateTime.now(ZoneOffset.UTC))
                .reason(motivo)
                .build();
        historyRepository.save(history);
    }

    private DisputeResponse toResponse(Dispute dispute) {
        String resolucion = dispute.getAdminResolution() != null
                ? dispute.getAdminResolution()
                : "Pendiente de resolucion";

        return new DisputeResponse(
                dispute.getId(),
                dispute.getAuction().getId(),
                dispute.getInitiator().getUsername(),
                dispute.getReason(),
                dispute.getDescription(),
                dispute.getCreationDate(),
                resolucion
        );
    }
}