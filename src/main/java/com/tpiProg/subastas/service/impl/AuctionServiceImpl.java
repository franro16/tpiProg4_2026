package com.tpiProg.subastas.service.impl;

import com.tpiProg.subastas.domain.entity.Auction;
import com.tpiProg.subastas.domain.entity.AuctionStateHistory;
import com.tpiProg.subastas.domain.entity.Product;
import com.tpiProg.subastas.domain.entity.User;
import com.tpiProg.subastas.domain.enums.AuctionStatus;
import com.tpiProg.subastas.dto.request.AuctionRequest;
import com.tpiProg.subastas.dto.request.CancellationRequest;
import com.tpiProg.subastas.dto.response.AuctionDetailResponse;
import com.tpiProg.subastas.dto.response.AuctionResponse;
import com.tpiProg.subastas.dto.response.AuctionStateHistoryResponse;
import com.tpiProg.subastas.exception.AuctionStateException;
import com.tpiProg.subastas.exception.BusinessException;
import com.tpiProg.subastas.exception.ResourceNotFoundException;
import com.tpiProg.subastas.exception.UnauthorizedException;
import com.tpiProg.subastas.repository.AuctionRepository;
import com.tpiProg.subastas.repository.AuctionStateHistoryRepository;
import com.tpiProg.subastas.repository.BidRepository;
import com.tpiProg.subastas.repository.ProductRepository;
import com.tpiProg.subastas.repository.UserRepository;
import com.tpiProg.subastas.service.AuctionService;
import com.tpiProg.subastas.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionServiceImpl implements AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionStateHistoryRepository historyRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BidRepository bidRepository;
    private final NotificationService notificationService;

    // -------------------------------------------------------
    // CREAR subasta en estado BORRADOR
    // -------------------------------------------------------
    @Override
    @Transactional
    public AuctionResponse create(AuctionRequest request, Long sellerId) {

        if (!request.endDate().isAfter(request.startDate())) {
            throw new BusinessException(
                    "La fecha de cierre debe ser posterior a la fecha de inicio");
        }

        User seller = findUserOrThrow(sellerId);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto", request.productId()));

        if (!product.getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedException("No sos el propietario de este producto");
        }

        Auction auction = Auction.builder()
                .product(product)
                .basePrice(request.basePrice())
                .minimumIncrement(request.minimumIncrement())
                .currentPrice(request.basePrice())
                // ACA ESTA LA CORRECCION: Le agregamos -03:00 y luego pasamos a UTC
                .startDate(request.startDate().atOffset(ZoneOffset.of("-03:00")).withOffsetSameInstant(ZoneOffset.UTC))
                .endDate(request.endDate().atOffset(ZoneOffset.of("-03:00")).withOffsetSameInstant(ZoneOffset.UTC))
                .status(AuctionStatus.BORRADOR)
                .description(request.description())
                .build();

        auctionRepository.save(auction);
        registrarHistorial(auction, null, AuctionStatus.BORRADOR, seller,
                "Subasta creada en borrador");

        log.debug("Subasta creada id={} por seller={}", auction.getId(), sellerId);
        return toResponse(auction);
    }

    // -------------------------------------------------------
    // PUBLICAR: BORRADOR -> PUBLICADA
    // -------------------------------------------------------
    @Override
    @Transactional
    public AuctionResponse publish(Long auctionId, Long sellerId) {
        Auction auction = findOrThrow(auctionId);
        User seller = findUserOrThrow(sellerId);

        if (!auction.getProduct().getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedException("No sos el propietario de esta subasta");
        }

        if (auction.getStatus() != AuctionStatus.BORRADOR) {
            throw new AuctionStateException(
                    "Solo se puede publicar una subasta en estado BORRADOR. Estado actual: "
                            + auction.getStatus());
        }

        AuctionStatus anterior = auction.getStatus();
        auction.cambiarEstado(AuctionStatus.PUBLICADA);
        auctionRepository.save(auction);

        registrarHistorial(auction, anterior, AuctionStatus.PUBLICADA, seller,
                "Subasta publicada por el vendedor");

        log.debug("Subasta id={} publicada por seller={}", auctionId, sellerId);
        return toResponse(auction);
    }

    // -------------------------------------------------------
    // OBTENER por id con privacidad segun rol
    // -------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public AuctionDetailResponse getById(Long auctionId, Long requestingUserId) {
        Auction auction = findOrThrow(auctionId);
        String winnerUsername = resolveWinnerVisibility(auction, requestingUserId);
        return toDetailResponse(auction, winnerUsername);
    }

    // -------------------------------------------------------
    // LISTAR todas
    // -------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<AuctionResponse> getAll() {
        return auctionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------
    // CANCELAR
    // -------------------------------------------------------
    @Override
    @Transactional
    public void cancel(Long auctionId, Long userId, boolean isAdmin,
                       CancellationRequest request) {
        Auction auction = findOrThrow(auctionId);
        User responsable = findUserOrThrow(userId);

        if (auction.getStatus() == AuctionStatus.FINALIZADA
                || auction.getStatus() == AuctionStatus.ADJUDICADA
                || auction.getStatus() == AuctionStatus.CANCELADA) {
            throw new AuctionStateException(
                    "No se puede cancelar una subasta en estado: " + auction.getStatus());
        }

        boolean tienePujas = bidRepository.existsByAuctionId(auctionId);

        if (!isAdmin) {
            if (!auction.getProduct().getSeller().getId().equals(userId)) {
                throw new UnauthorizedException("No sos el propietario de esta subasta");
            }
            if (tienePujas) {
                throw new BusinessException(
                        "No podés cancelar una subasta con pujas. Contactá a un administrador.");
            }
        }

        AuctionStatus anterior = auction.getStatus();
        auction.cambiarEstado(AuctionStatus.CANCELADA);
        auctionRepository.save(auction);

        registrarHistorial(auction, anterior, AuctionStatus.CANCELADA, responsable,
                request.reason());

        log.debug("Subasta id={} cancelada por userId={}", auctionId, userId);
    }

    // -------------------------------------------------------
    // HISTORIAL
    // -------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<AuctionStateHistoryResponse> getHistory(Long auctionId) {
        if (!auctionRepository.existsById(auctionId)) {
            throw new ResourceNotFoundException("Subasta", auctionId);
        }
        return historyRepository.findByAuctionIdOrderByChangeDateDesc(auctionId).stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------
    // TRANSICIONES AUTOMATICAS - llamado por el Scheduler
    // -------------------------------------------------------
    @Override
    @Transactional
    public void processScheduledTransitions() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<Auction> auctions = auctionRepository.findAll();

        for (Auction auction : auctions) {
            try {
                procesarTransicion(auction, now);
            } catch (Exception e) {
                log.error("Error en transicion automatica subasta id={}: {}",
                        auction.getId(), e.getMessage());
            }
        }
    }

    // -------------------------------------------------------
    // LOGICA DE TRANSICION INDIVIDUAL
    // -------------------------------------------------------
    private void procesarTransicion(Auction auction, OffsetDateTime now) {
        User sistemaUser = userRepository.findById(1L)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario sistema", 1L));

        if (auction.getStatus() == AuctionStatus.PUBLICADA
                && (now.isAfter(auction.getStartDate())
                || now.isEqual(auction.getStartDate()))) {

            AuctionStatus anterior = auction.getStatus();
            auction.cambiarEstado(AuctionStatus.ACTIVA);
            auctionRepository.save(auction);
            registrarHistorial(auction, anterior, AuctionStatus.ACTIVA, sistemaUser,
                    "Inicio automatico por fecha de inicio alcanzada");

            log.debug("Subasta id={} activada automaticamente", auction.getId());

        } else if (auction.getStatus() == AuctionStatus.ACTIVA
                && auction.evaluarCierreAutomatico(now)) {

            boolean tienePujas = bidRepository.existsByAuctionId(auction.getId());
            AuctionStatus anterior = auction.getStatus();

            if (tienePujas) {
                auction.cambiarEstado(AuctionStatus.ADJUDICADA);
                auction.setAdjudicationDate(now);
                auctionRepository.save(auction);
                registrarHistorial(auction, anterior, AuctionStatus.ADJUDICADA, sistemaUser,
                        "Cierre automatico con pujas: subasta adjudicada");

                // Notificar al ganador
                if (auction.getWinner() != null) {
                    notificationService.createNotification(
                            auction.getWinner(),
                            "¡Felicitaciones! Ganaste la subasta del producto '"
                                    + auction.getProduct().getName()
                                    + "' con una oferta de $" + auction.getCurrentPrice()
                    );
                }

                // Notificar al vendedor
                notificationService.createNotification(
                        auction.getProduct().getSeller(),
                        "Tu subasta del producto '"
                                + auction.getProduct().getName()
                                + "' fue adjudicada por $" + auction.getCurrentPrice()
                );

                log.debug("Subasta id={} adjudicada. Notificaciones enviadas.", auction.getId());

            } else {
                auction.cambiarEstado(AuctionStatus.FINALIZADA);
                auctionRepository.save(auction);
                registrarHistorial(auction, anterior, AuctionStatus.FINALIZADA, sistemaUser,
                        "Cierre automatico sin pujas: subasta finalizada");

                // Notificar al vendedor que no hubo pujas
                notificationService.createNotification(
                        auction.getProduct().getSeller(),
                        "Tu subasta del producto '"
                                + auction.getProduct().getName()
                                + "' finalizó sin recibir pujas."
                );

                log.debug("Subasta id={} finalizada sin pujas.", auction.getId());
            }
        }
    }

    // -------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------
    private Auction findOrThrow(Long id) {
        return auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta", id));
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    public void registrarHistorial(Auction auction, AuctionStatus estadoAnterior,
                                   AuctionStatus estadoNuevo, User responsable, String motivo) {
        AuctionStateHistory history = AuctionStateHistory.builder()
                .auction(auction)
                .previousState(estadoAnterior != null ? estadoAnterior : estadoNuevo)
                .newState(estadoNuevo)
                .changeDate(OffsetDateTime.now(ZoneOffset.UTC))
                .reason(motivo)
                .responsibleUser(responsable)
                .build();

        historyRepository.save(history);
    }

    private String resolveWinnerVisibility(Auction auction, Long requestingUserId) {
        if (auction.getWinner() == null) return null;

        if (auction.getStatus() == AuctionStatus.ADJUDICADA
                || auction.getStatus() == AuctionStatus.FINALIZADA
                || auction.getStatus() == AuctionStatus.EN_DISPUTA) {
            return auction.getWinner().getUsername();
        }

        Long sellerId = auction.getProduct().getSeller().getId();
        Long winnerId = auction.getWinner().getId();

        if (requestingUserId != null
                && (requestingUserId.equals(sellerId) || requestingUserId.equals(winnerId))) {
            return auction.getWinner().getUsername();
        }

        return null;
    }

    // -------------------------------------------------------
    // MAPPERS
    // -------------------------------------------------------
    private AuctionResponse toResponse(Auction a) {
        return new AuctionResponse(
                a.getId(),
                a.getProduct().getId(),
                a.getProduct().getName(),
                a.getBasePrice(),
                a.getCurrentPrice(),
                a.getMinimumIncrement(),
                a.getStartDate(),
                a.getEndDate(),
                a.getStatus().name(),
                a.getDescription()
        );
    }

    private AuctionDetailResponse toDetailResponse(Auction a, String winnerUsername) {
        return new AuctionDetailResponse(
                a.getId(),
                a.getProduct().getId(),
                a.getProduct().getName(),
                a.getProduct().getSeller().getUsername(),
                a.getBasePrice(),
                a.getCurrentPrice(),
                a.getMinimumIncrement(),
                a.getStartDate(),
                a.getEndDate(),
                a.getAdjudicationDate(),
                a.getStatus().name(),
                a.getDescription(),
                winnerUsername
        );
    }

    private AuctionStateHistoryResponse toHistoryResponse(AuctionStateHistory h) {
        return new AuctionStateHistoryResponse(
                h.getId(),
                h.getPreviousState().name(),
                h.getNewState().name(),
                h.getChangeDate(),
                h.getReason(),
                h.getResponsibleUser().getUsername()
        );
    }
}