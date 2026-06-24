package com.tpiProg.subastas.mapper;

import com.tpiProg.subastas.domain.entity.Product;
import com.tpiProg.subastas.dto.response.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory().getName(), // Extraemos el nombre directamente de la relación
                product.getSeller().getUsername() // Extraemos el nombre del vendedor
        );
    }
}