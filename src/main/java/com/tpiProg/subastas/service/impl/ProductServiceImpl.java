package com.tpiProg.subastas.service.impl;

import com.tpiProg.subastas.domain.entity.Category;
import com.tpiProg.subastas.domain.entity.Product;
import com.tpiProg.subastas.domain.entity.User;
import com.tpiProg.subastas.dto.request.ProductRequest;
import com.tpiProg.subastas.dto.response.ProductResponse;
import com.tpiProg.subastas.exception.ResourceNotFoundException;
import com.tpiProg.subastas.exception.UnauthorizedException;
import com.tpiProg.subastas.mapper.ProductMapper;
import com.tpiProg.subastas.repository.CategoryRepository;
import com.tpiProg.subastas.repository.ProductRepository;
import com.tpiProg.subastas.repository.UserRepository;
import com.tpiProg.subastas.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, String username) {
        // traemos el user logueado
        User seller = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", 0L));

        // buscamos la categoria, si no esta tira el 404 del global handler
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", request.categoryId()));

        // armamos la entidad limpia para la base
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .seller(seller)
                .category(category)
                .build();

        productRepository.save(product);

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, String username) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));

        // chequeamos que no venga un vivo a editar lo que no es suyo
        if (!product.getSeller().getUsername().equals(username)) {
            throw new UnauthorizedException("No podés editar un producto que no es tuyo");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", request.categoryId()));

        // pisamos los datos viejos
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategory(category);

        productRepository.save(product);

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id, String username) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));

        // validamos propiedad de nuevo por las dudas
        if (!product.getSeller().getUsername().equals(username)) {
            throw new UnauthorizedException("No podés borrar un producto que no es tuyo");
        }

        // ojo acá: si el producto ya está en una subasta, la base va a chillar por la foreign key.
        // lo dejamos así para que lo ataje la excepción genérica y no rompa todo el hilo.
        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        // devolvemos todo mapeadito a DTO para no filtrar datos de mas
        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }
}