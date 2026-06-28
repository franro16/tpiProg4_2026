package com.tpiProg.subastas.service;

import com.tpiProg.subastas.dto.request.CategoryRequest;
import com.tpiProg.subastas.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    void deleteCategory(Long id);
    CategoryResponse getCategoryById(Long id);
    List<CategoryResponse> getAllCategories();
}