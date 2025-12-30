package com.cuttypaws.service.interf;

import com.cuttypaws.dto.*;
import com.cuttypaws.response.CategoryResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CategoryService {

    CategoryResponse createCategory(CategoryDto categoryRequest, MultipartFile imageFile);

    CategoryResponse updateCategory(Long categoryId,CategoryDto categoryRequest, MultipartFile imageFile);

    CategoryResponse getAllCategory();

    CategoryResponse getCategoryById(Long categoryId);

    CategoryResponse deleteCategory(Long categoryId);

    CategoryResponse getCategoryByIdWithSubCategories(Long categoryId);

    CategoryResponse searchCategories(String query);

}






