package com.tpiProg.subastas.service.impl;

import com.tpiProg.subastas.domain.entity.Auction;
import com.tpiProg.subastas.domain.entity.Bid;
import com.tpiProg.subastas.domain.entity.User;
import com.tpiProg.subastas.domain.enums.AuctionStatus;
import com.tpiProg.subastas.dto.request.BidRequest;
import com.tpiProg.subastas.dto.response.BidResponse;
import com.tpiProg.subastas.exception.AuctionStateException;
import com.tpiProg.subastas.exception.BidException;
import com.tpiProg.subastas.exception.BusinessException;
import com.tpiProg.subastas.exception.ResourceNotFoundException;
import com.tpiProg.subastas.exception.UnauthorizedException;
import com.tpiProg.subastas.mapper.BidMapper;
import com.tpiProg.subastas.repository.AuctionRepository;
import com.tpiProg.subastas.repository.BidRepository;
import com.tpiProg.subastas.repository.UserRepository;
import com.tpiProg.subastas.service.BidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidServiceImpl implements BidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final BidMapper bidMapper;

    @Override
    @Transactional
    public BidResponse placeBid(Long auctionId, BidRequest request, String userEmail) {

        // Cargar usuario por email (authentication.getName() devuelve email en UserPrincipal)
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado", 0L));

        if (user.isBlocked()) {
            throw new UnauthorizedException("El usuario se encuentra bloqueado y no puede participar en subastas.");
        }

        // Locking pesimista: bloquea la fila en PostgreSQL hasta que termine la transaccion
        // Si dos usuarios pujan al mismo tiempo, el segundo espera que termine el primero
        Auction auction = auctionRepository.findByIdWithPessimisticLock(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta", auctionId));

        if (auction.getStatus() != AuctionStatus.ACTIVA) {
            throw new AuctionStateException("No se aceptan pujas. La subasta no está ACTIVA.");
        }

        // Usamos fecha del SERVIDOR en UTC, nunca la del cliente
        if (OffsetDateTime.now(ZoneOffset.UTC).isAfter(auction.getEndDate())) {
            throw new AuctionStateException("El periodo para pujar en esta subasta ha finalizado.");
        }

        if (user.getId().equals(auction.getProduct().getSeller().getId())) {
            throw new BusinessException("El vendedor no puede pujar en su propia subasta.");
        }

        // Validar monto contra la BD, no contra lo que el cliente cree que es el precio actual
        boolean isFirstBid = !bidRepository.existsByAuctionId(auctionId);
        if (isFirstBid) {
            if (request.amount().compareTo(auction.getBasePrice()) < 0) {
                throw new BidException("La primera puja debe ser igual o superior al precio base de la subasta.");
            }
        } else {
            if (request.amount().compareTo(auction.getCurrentPrice().add(auction.getMinimumIncrement())) < 0) {
                throw new BidException("La puja debe ser igual o superior al precio actual más el incremento mínimo.");
            }
        }

        Bid bid = Bid.builder()
                .auction(auction)
                .user(user)
                .amount(request.amount())
                .bidDate(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        bidRepository.save(bid);

        // Actualizar precio actual y ganador en la misma transaccion atomica
        auction.setCurrentPrice(request.amount());
        auction.setWinner(user);
        auctionRepository.save(auction);

        log.info("Puja registrada: Subasta ID={}, Usuario={}, Monto={}", auctionId, userEmail, request.amount());

        return bidMapper.toResponse(bid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BidResponse> getMyBids(Long auctionId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado", 0L));

        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId)
                .stream()
                .filter(bid -> bid.getUser().getId().equals(user.getId()))
                .map(bidMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BidResponse> getAuctionBids(Long auctionId, Authentication authentication) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta", auctionId));

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        // Comparar por email porque authentication.getName() devuelve email
        boolean isSeller = auction.getProduct().getSeller().getEmail()
                .equals(authentication.getName());

        if (!isAdmin) {
            if (!isSeller) {
                throw new UnauthorizedException("No tenés permisos para ver el historial completo de pujas.");
            }
            if (auction.getStatus() != AuctionStatus.FINALIZADA
                    && auction.getStatus() != AuctionStatus.ADJUDICADA) {
                throw new BusinessException("Como vendedor, solo podés ver las pujas una vez que la subasta haya finalizado.");
            }
        }

        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId)
                .stream()
                .map(bidMapper::toResponse)
                .collect(Collectors.toList());
    }
}