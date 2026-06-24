package com.tpiProg.subastas.repository;

import com.tpiProg.subastas.domain.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
}