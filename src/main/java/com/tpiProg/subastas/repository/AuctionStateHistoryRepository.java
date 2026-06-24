package com.tpiProg.subastas.repository;

import com.tpiProg.subastas.domain.entity.AuctionStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuctionStateHistoryRepository extends JpaRepository<AuctionStateHistory, Long> {
    // Para buscar todo el historial de cambios de una subasta específica
    List<AuctionStateHistory> findByAuctionIdOrderByChangeDateDesc(Long auctionId);
}