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
    private final BidMapper bidMapper; // Utiliza el mapper creado por Victoria

    @Override
    @Transactional
    public BidResponse placeBid(Long auctionId, BidRequest request, String username) {
        
        // 1. Obtener usuario (el filtro JWT ya validó que existe, pero necesitamos la entidad)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", 0L));

        // 2. Validar que el usuario no esté bloqueado
        if (user.isBlocked()) {
            throw new UnauthorizedException("El usuario se encuentra bloqueado y no puede participar en subastas.");
        }

        // 3. Obtener subasta aplicando LOCKING PESIMISTA
        // Esto bloquea la fila en PostgreSQL. Si entra otra petición concurrente, quedará en espera.
        Auction auction = auctionRepository.findByIdWithPessimisticLock(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta", auctionId));

        // 4. Validaciones de estado y tiempo
        if (auction.getStatus() != AuctionStatus.ACTIVA) {
            throw new AuctionStateException("No se aceptan pujas. La subasta no está ACTIVA.");
        }

        if (OffsetDateTime.now(ZoneOffset.UTC).isAfter(auction.getEndDate())) {
            throw new AuctionStateException("El periodo para pujar en esta subasta ha finalizado.");
        }

        // 5. Validar que el vendedor no puje por su propio producto
        if (user.getId().equals(auction.getProduct().getSeller().getId())) {
            throw new BusinessException("El vendedor no puede pujar en su propia subasta.");
        }

        // 6. Validar lógicas de montos exactos con BigDecimal
        boolean isFirstBid = !bidRepository.existsByAuctionId(auctionId);
        if (isFirstBid) {
            // Primera puja: Debe ser mayor o igual al precio base
            if (request.amount().compareTo(auction.getBasePrice()) < 0) {
                throw new BidException("La primera puja debe ser igual o superior al precio base de la subasta.");
            }
        } else {
            // Pujas subsiguientes: Debe superar el precio actual por al menos el incremento mínimo
            if (request.amount().compareTo(auction.getCurrentPrice().add(auction.getMinimumIncrement())) < 0) {
                throw new BidException("La puja debe ser igual o superior al precio actual más el incremento mínimo estipulado.");
            }
        }

        // 7. Crear la entidad Bid confiando siempre en el OffsetDateTime del Servidor en UTC
        Bid bid = Bid.builder()
                .auction(auction)
                .user(user)
                .amount(request.amount())
                .bidDate(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        bidRepository.save(bid);

        // 8. Actualizar la subasta en la misma transacción
        auction.setCurrentPrice(request.amount());
        auction.setWinner(user);
        auctionRepository.save(auction);

        log.info("Nueva puja registrada exitosamente: Subasta ID {}, Usuario {}, Monto {}", 
                 auctionId, username, request.amount());

        return bidMapper.toResponse(bid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BidResponse> getMyBids(Long auctionId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", 0L));

        // Filtramos en memoria las pujas de este usuario para la subasta indicada
        // Opcionalmente, podrías crear un método findByAuctionIdAndUserId en BidRepository para optimizar
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

        boolean isSeller = auction.getProduct().getSeller().getUsername().equals(authentication.getName());

        // Regla: Solo ADMIN puede ver todas las pujas libremente.
        // El SELLER solo puede verlas si la subasta ya finalizó o se adjudicó.
        if (!isAdmin) {
            if (!isSeller) {
                throw new UnauthorizedException("No tienes permisos para ver el historial completo de pujas.");
            }
            if (auction.getStatus() != AuctionStatus.FINALIZADA && auction.getStatus() != AuctionStatus.ADJUDICADA) {
                throw new BusinessException("Como vendedor, solo podrás ver las pujas una vez que la subasta haya finalizado.");
            }
        }

        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId)
                .stream()
                .map(bidMapper::toResponse)
                .collect(Collectors.toList());
    }
}