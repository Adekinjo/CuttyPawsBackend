package com.cuttypaws.service.interf;

import com.cuttypaws.dto.*;
import com.cuttypaws.response.SubCategoryResponse;
import org.springframework.web.multipart.MultipartFile;

public interface SubCategoryService {

    SubCategoryResponse createSubCategory(SubCategoryDto subCategoryRequest, MultipartFile imageFile);

    SubCategoryResponse updateSubCategory(Long subCategoryId, SubCategoryDto subCategoryRequest, MultipartFile imageFile);

    SubCategoryResponse deleteSubCategory(Long subCategoryId);

    SubCategoryResponse getSubCategoryById(Long subCategoryId);
    SubCategoryResponse searchSubCategories(String query);

    SubCategoryResponse getAllSubCategories();

    SubCategoryResponse getSubCategoriesByCategory(Long categoryId);
}