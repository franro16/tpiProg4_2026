package com.tpiProg.subastas.service;

import com.tpiProg.subastas.dto.request.ProductRequest;
import com.tpiProg.subastas.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request, String username);
    ProductResponse updateProduct(Long id, ProductRequest request, String username);
    void deleteProduct(Long id, String username);
    ProductResponse getProductById(Long id);
    List<ProductResponse> getAllProducts();
}