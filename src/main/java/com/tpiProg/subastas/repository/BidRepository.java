package com.tpiProg.subastas.repository;

import com.tpiProg.subastas.domain.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {

    // Busca la puja ganadora (la mas alta) de una subasta especifica
    Optional<Bid> findTopByAuctionIdOrderByAmountDesc(Long auctionId);

    // Trae todas las pujas que hizo un usuario (para ver su historial)
    List<Bid> findByUserIdOrderByBidDateDesc(Long userId);

    // Verifica si una subasta tiene al menos una puja registrada
    boolean existsByAuctionId(Long auctionId);

    // Todas las pujas de una subasta ordenadas de mayor a menor monto
    List<Bid> findByAuctionIdOrderByAmountDesc(Long auctionId);
}