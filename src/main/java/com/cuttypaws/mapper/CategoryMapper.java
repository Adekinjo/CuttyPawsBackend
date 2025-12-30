package com.cuttypaws.mapper;

import com.cuttypaws.dto.CategoryDto;
import com.cuttypaws.dto.SubCategoryDto;
import com.cuttypaws.entity.Category;
import com.cuttypaws.entity.SubCategory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategoryMapper {


    // Add this method to map SubCategory to SubCategoryDto
    public SubCategoryDto mapSubCategoryToDto(SubCategory subCategory) {
        SubCategoryDto subCategoryDto = new SubCategoryDto();
        subCategoryDto.setId(subCategory.getId());
        subCategoryDto.setName(subCategory.getName());
        subCategoryDto.setImageUrl(subCategory.getImageUrl());
        subCategoryDto.setCategoryId(subCategory.getCategory().getId());

        return subCategoryDto;
    }

    // Update the mapCategoryToDtoBasic method to include subcategories
    public CategoryDto mapCategoryToDtoBasic(Category category) {
        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(category.getId());
        categoryDto.setName(category.getName());
        categoryDto.setImageUrl(category.getImageUrl());

        if (category.getSubCategories() != null) {
            List<SubCategoryDto> subCategoryDtos = category.getSubCategories().stream()
                    .map(this::mapSubCategoryToDto)
                    .collect(Collectors.toList());
            categoryDto.setSubCategories(subCategoryDtos);
        }
        return categoryDto;
    }
}
