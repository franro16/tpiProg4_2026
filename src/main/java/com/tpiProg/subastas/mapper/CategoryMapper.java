package com.tpiProg.subastas.mapper;

import com.tpiProg.subastas.domain.entity.Category;
import com.tpiProg.subastas.dto.response.CategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    
    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}