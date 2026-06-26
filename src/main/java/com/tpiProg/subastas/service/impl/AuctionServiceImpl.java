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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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

    // -------------------------------------------------------
    // CREAR subasta en estado BORRADOR
    // -------------------------------------------------------
    @Override
    @Transactional
    public AuctionResponse create(AuctionRequest request, Long sellerId) {

        // Validar que la fecha de cierre sea posterior a la de inicio
        if (!request.endDate().isAfter(request.startDate())) {
            throw new BusinessException("La fecha de cierre debe ser posterior a la fecha de inicio");
        }

        User seller = findUserOrThrow(sellerId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto", request.productId()));

        // Solo el dueño del producto puede crear la subasta
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedException("No sos el propietario de este producto");
        }

        Auction auction = Auction.builder()
                .product(product)
                .basePrice(request.basePrice())
                .minimumIncrement(request.minimumIncrement())
                .currentPrice(request.basePrice())
                .startDate(request.startDate().withOffsetSameInstant(java.time.ZoneOffset.UTC))
                .endDate(request.endDate().withOffsetSameInstant(java.time.ZoneOffset.UTC))
                .status(AuctionStatus.BORRADOR)
                .description(request.description())
                .build();

        auctionRepository.save(auction);

        registrarHistorial(auction, null, AuctionStatus.BORRADOR, seller,
                "Subasta creada en borrador");

        log.debug("Subasta creada con id={} por seller={}", auction.getId(), sellerId);
        return toResponse(auction);
    }

    // -------------------------------------------------------
    // PUBLICAR subasta: BORRADOR -> PUBLICADA
    // -------------------------------------------------------
    @Override
    @Transactional
    public AuctionResponse publish(Long auctionId, Long sellerId) {
        Auction auction = findOrThrow(auctionId);
        User seller = findUserOrThrow(sellerId);

        // Solo el vendedor dueño puede publicar
        if (!auction.getProduct().getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedException("No sos el propietario de esta subasta");
        }

        if (auction.getStatus() != AuctionStatus.BORRADOR) {
            throw new AuctionStateException(
                    "Solo se puede publicar una subasta en estado BORRADOR. Estado actual: "
                            + auction.getStatus());
        }

        AuctionStatus estadoAnterior = auction.getStatus();
        auction.cambiarEstado(AuctionStatus.PUBLICADA);
        auctionRepository.save(auction);

        registrarHistorial(auction, estadoAnterior, AuctionStatus.PUBLICADA, seller,
                "Subasta publicada por el vendedor");

        log.debug("Subasta id={} publicada por seller={}", auctionId, sellerId);
        return toResponse(auction);
    }

    // -------------------------------------------------------
    // OBTENER subasta por id con privacidad segun rol
    // -------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public AuctionDetailResponse getById(Long auctionId, Long requestingUserId) {
        Auction auction = findOrThrow(auctionId);

        // Determinar ganador visible segun estado y rol
        String winnerUsername = resolveWinnerVisibility(auction, requestingUserId);

        return toDetailResponse(auction, winnerUsername);
    }

    // -------------------------------------------------------
    // LISTAR todas las subastas (vista publica reducida)
    // -------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<AuctionResponse> getAll() {
        return auctionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------
    // CANCELAR subasta
    // Regla: vendedor puede cancelar solo si no tiene pujas
    //        ADMIN puede cancelar aunque tenga pujas
    // -------------------------------------------------------
    @Override
    @Transactional
    public void cancel(Long auctionId, Long userId, boolean isAdmin, CancellationRequest request) {
        Auction auction = findOrThrow(auctionId);
        User responsable = findUserOrThrow(userId);

        // Estados desde los que se puede cancelar
        if (auction.getStatus() == AuctionStatus.FINALIZADA
                || auction.getStatus() == AuctionStatus.ADJUDICADA
                || auction.getStatus() == AuctionStatus.CANCELADA) {
            throw new AuctionStateException(
                    "No se puede cancelar una subasta en estado: " + auction.getStatus());
        }

        boolean tienePujas = bidRepository.existsByAuctionId(auctionId);

        if (!isAdmin) {
            // Verificar que sea el vendedor
            if (!auction.getProduct().getSeller().getId().equals(userId)) {
                throw new UnauthorizedException("No sos el propietario de esta subasta");
            }
            if (tienePujas) {
                throw new BusinessException(
                        "No podés cancelar una subasta que ya tiene pujas. Contactá a un administrador.");
            }
        }

        AuctionStatus estadoAnterior = auction.getStatus();
        auction.cambiarEstado(AuctionStatus.CANCELADA);
        auctionRepository.save(auction);

        registrarHistorial(auction, estadoAnterior, AuctionStatus.CANCELADA, responsable,
                request.reason());

        log.debug("Subasta id={} cancelada por userId={} (isAdmin={})", auctionId, userId, isAdmin);
    }

    // -------------------------------------------------------
    // HISTORIAL de cambios de estado
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
    // PUBLICADA -> ACTIVA si llego la fecha de inicio
    // ACTIVA -> FINALIZADA si no hay pujas
    // ACTIVA -> ADJUDICADA si hay pujas
    // -------------------------------------------------------
    @Override
    @Transactional
    public void processScheduledTransitions() {
        OffsetDateTime now = OffsetDateTime.now(java.time.ZoneOffset.UTC);
        List<Auction> auctions = auctionRepository.findAll();

        for (Auction auction : auctions) {
            try {
                procesarTransicion(auction, now);
            } catch (Exception e) {
                // Loguear pero no detener el scheduler por una subasta con error
                log.error("Error procesando transicion automatica para subasta id={}: {}",
                        auction.getId(), e.getMessage());
            }
        }
    }

    // -------------------------------------------------------
    // METODO INTERNO: logica de transicion para una subasta
    // -------------------------------------------------------
    private void procesarTransicion(Auction auction, OffsetDateTime now) {
        // Usuario sistema para el historial de transiciones automaticas
        // Usamos el id 1 que corresponde al ADMIN inicial cargado por Flyway
        User sistemaUser = userRepository.findById(1L)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario sistema", 1L));

        if (auction.getStatus() == AuctionStatus.PUBLICADA
                && (now.isAfter(auction.getStartDate()) || now.isEqual(auction.getStartDate()))) {

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
                log.debug("Subasta id={} adjudicada automaticamente", auction.getId());
            } else {
                auction.cambiarEstado(AuctionStatus.FINALIZADA);
                auctionRepository.save(auction);
                registrarHistorial(auction, anterior, AuctionStatus.FINALIZADA, sistemaUser,
                        "Cierre automatico sin pujas: subasta finalizada");
                log.debug("Subasta id={} finalizada automaticamente sin pujas", auction.getId());
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

    // Registra un cambio de estado en el historial de auditoria
    public void registrarHistorial(Auction auction, AuctionStatus estadoAnterior,
                                    AuctionStatus estadoNuevo, User responsable, String motivo) {
        AuctionStateHistory history = AuctionStateHistory.builder()
                .auction(auction)
                .previousState(estadoAnterior != null ? estadoAnterior : estadoNuevo)
                .newState(estadoNuevo)
                .changeDate(OffsetDateTime.now(java.time.ZoneOffset.UTC))
                .reason(motivo)
                .responsibleUser(responsable)
                .build();
        historyRepository.save(history);
    }

    // Privacidad: mientras esta ACTIVA, el ganador parcial no se muestra a cualquiera
    private String resolveWinnerVisibility(Auction auction, Long requestingUserId) {
        if (auction.getWinner() == null) return null;

        // Si ya termino, el ganador es visible para todos
        if (auction.getStatus() == AuctionStatus.ADJUDICADA
                || auction.getStatus() == AuctionStatus.FINALIZADA
                || auction.getStatus() == AuctionStatus.EN_DISPUTA) {
            return auction.getWinner().getUsername();
        }

        // Durante subasta activa: solo el propio ganador o el vendedor ven el nombre
        Long sellerId = auction.getProduct().getSeller().getId();
        Long winnerId = auction.getWinner().getId();
        if (requestingUserId != null
                && (requestingUserId.equals(sellerId) || requestingUserId.equals(winnerId))) {
            return auction.getWinner().getUsername();
        }

        return null;
    }

    // -------------------------------------------------------
    // MAPPERS internos
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