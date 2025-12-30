package com.cuttypaws.mapper;

import com.cuttypaws.dto.SubCategoryDto;
import com.cuttypaws.entity.SubCategory;
import org.springframework.stereotype.Component;

@Component
public class SubCategoryMapper {

    // Add this method to map SubCategory to SubCategoryDto
    public SubCategoryDto mapSubCategoryToDto(SubCategory subCategory) {
        SubCategoryDto subCategoryDto = new SubCategoryDto();
        subCategoryDto.setId(subCategory.getId());
        subCategoryDto.setName(subCategory.getName());
        subCategoryDto.setImageUrl(subCategory.getImageUrl());
        subCategoryDto.setCategoryId(subCategory.getCategory().getId());

        return subCategoryDto;
    }

}
