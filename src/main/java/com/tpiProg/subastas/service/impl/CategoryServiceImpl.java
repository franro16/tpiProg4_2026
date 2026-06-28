package com.tpiProg.subastas.service.impl;

import com.tpiProg.subastas.domain.entity.Category;
import com.tpiProg.subastas.dto.request.CategoryRequest;
import com.tpiProg.subastas.dto.response.CategoryResponse;
import com.tpiProg.subastas.exception.ResourceNotFoundException;
import com.tpiProg.subastas.mapper.CategoryMapper;
import com.tpiProg.subastas.repository.CategoryRepository;
import com.tpiProg.subastas.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper; // Usamos el traductor que armó Vicky

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        // armamos la entidad desde cero con lo que manda el admin
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();
        
        categoryRepository.save(category);
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id));

        // pisamos los datos viejos con los nuevos
        category.setName(request.name());
        category.setDescription(request.description());
        
        categoryRepository.save(category);
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id));
        
        // si la categoría ya tiene productos enganchados, la base de datos va a chillar por la foreign key.
        // el GlobalExceptionHandler ataja esa excepción y tira un 500 para no romper el programa.
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        // traemos todas y las pasamos a DTO para que salgan limpias
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }
}