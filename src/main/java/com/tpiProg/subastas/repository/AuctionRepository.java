package com.tpiProg.subastas.repository;

import com.tpiProg.subastas.domain.entity.Auction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    // ¡ÉSTE ES EL CORAZÓN DE LA CONCURRENCIA!
    // PESSIMISTIC_WRITE bloquea la fila en la base de datos hasta que termine la transacción.
    // Si dos personas pujan al mismo tiempo exacto, el segundo esperará a que termine el primero.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdWithPessimisticLock(@Param("id") Long id);
}