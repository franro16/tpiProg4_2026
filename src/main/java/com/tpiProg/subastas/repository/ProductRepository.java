package com.tpiProg.subastas.repository;

import com.tpiProg.subastas.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}