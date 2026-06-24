package com.tpiProg.subastas.dto.response;

// Respuesta simplificada de un producto (evitamos mandar toda la info confidencial del vendedor)
public record ProductResponse(
        Long id,
        String name,
        String description,
        String categoryName,
        String sellerUsername
) {}