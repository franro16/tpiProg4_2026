package com.tpiProg.subastas.controller;

import com.tpiProg.subastas.dto.request.ProductRequest;
import com.tpiProg.subastas.dto.response.ProductResponse;
import com.tpiProg.subastas.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ------------------------------------
    // ENDPOINTS PÚBLICOS
    // ------------------------------------
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // ------------------------------------
    // ENDPOINTS SOLO PARA VENDEDORES
    // ------------------------------------
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(productService.updateProduct(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication) {
        productService.deleteProduct(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}