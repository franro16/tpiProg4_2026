package com.tpiProg.subastas.service.impl;

import com.tpiProg.subastas.domain.entity.Category;
import com.tpiProg.subastas.domain.entity.Product;
import com.tpiProg.subastas.domain.entity.User;
import com.tpiProg.subastas.dto.request.ProductRequest;
import com.tpiProg.subastas.dto.response.ProductResponse;
import com.tpiProg.subastas.exception.BusinessException;
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
    public ProductResponse createProduct(ProductRequest request, String userEmail) {
        // authentication.getName() devuelve email, no username
        User seller = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado", 0L));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", request.categoryId()));

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
    public ProductResponse updateProduct(Long id, ProductRequest request, String userEmail) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));

        // comparar por email porque eso devuelve authentication.getName()
        if (!product.getSeller().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("No podés editar un producto que no es tuyo");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", request.categoryId()));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategory(category);
        productRepository.save(product);

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id, String userEmail) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));

        if (!product.getSeller().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("No podés borrar un producto que no es tuyo");
        }

        // Si el producto tiene una subasta activa, la FK de la BD va a rechazar el delete.
        // Lo captura el GlobalExceptionHandler como error 500 con mensaje claro.
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
        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }
}