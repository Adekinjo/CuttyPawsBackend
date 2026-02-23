package com.cuttypaws.service.impl;

import com.cuttypaws.cache.CacheMonitorService;
import com.cuttypaws.dto.*;

import com.cuttypaws.entity.*;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.mapper.CategoryMapper;
import com.cuttypaws.repository.*;
import com.cuttypaws.response.CategoryResponse;
import com.cuttypaws.service.AwsS3Service;
import com.cuttypaws.service.interf.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
// REMOVED @CacheConfig - it causes conflicts
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepo categoryRepo;
    private final CategoryMapper categoryMapper;
    private final AwsS3Service awsS3Service;
    //private final CacheMonitorService cacheMonitorService;

    @Override
    @Transactional
    @CacheEvict(value = {
            "allCategories", "categoryById",
            "allSubCategories", "subCategoriesByCategory",
            "categoryWithSubCategories", "categorySearch"
    }, allEntries = true)
    public CategoryResponse createCategory(CategoryDto categoryRequest, MultipartFile imageFile) {
        log.info("🆕 Creating new category: {}", categoryRequest.getName());

        Category category = new Category();
        category.setName(categoryRequest.getName());

        // Upload image to S3 if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageUrl = awsS3Service.uploadMedia(imageFile);
                category.setImageUrl(imageUrl);
                log.info("📸 Image uploaded successfully: {}", imageUrl);
            } catch (Exception e) {
                log.error("❌ Failed to upload category image: {}", e.getMessage());
                throw new RuntimeException("Failed to upload category image: " + e.getMessage());
            }
        }

        // Initialize the subCategories list if it's null
        if (category.getSubCategories() == null) {
            category.setSubCategories(new ArrayList<>());
        }

        // Add subcategories if provided
        if (categoryRequest.getSubCategories() != null) {
            for (SubCategoryDto subCategoryDto : categoryRequest.getSubCategories()) {
                SubCategory subCategory = new SubCategory();
                subCategory.setName(subCategoryDto.getName());
                category.addSubCategory(subCategory);
            }
        }

        // Save the category (cascade will save subcategories)
        categoryRepo.save(category);


        log.info("✅ Category created successfully: {}", categoryRequest.getName());
        return CategoryResponse.builder()
                .status(200)
                .timeStamp(LocalDateTime.now())
                .message("Category created successfully")
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "allCategories", "categoryById",
            "allSubCategories", "subCategoriesByCategory",
            "categoryWithSubCategories", "categorySearch"
    }, allEntries = true)
    public CategoryResponse updateCategory(Long categoryId, CategoryDto categoryRequest, MultipartFile imageFile) {
        log.info("🔄 Updating category: {}", categoryId);

        // Fetch the existing category
        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        // Update the category name
        if (categoryRequest.getName() != null) {
            category.setName(categoryRequest.getName());
        }

        // Update image if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageUrl = awsS3Service.uploadMedia(imageFile);
                category.setImageUrl(imageUrl);
                log.info("📸 Category image updated successfully: {}", imageUrl);
            } catch (Exception e) {
                log.error("❌ Failed to update category image: {}", e.getMessage());
                throw new RuntimeException("Failed to update category image: " + e.getMessage());
            }
        }

        // Initialize the subCategories list if it's null
        if (category.getSubCategories() == null) {
            category.setSubCategories(new ArrayList<>());
        }

        // Update subcategories
        if (categoryRequest.getSubCategories() != null) {
            // Create a map of existing subcategories for quick lookup
            Map<Long, SubCategory> existingSubCategories = category.getSubCategories().stream()
                    .collect(Collectors.toMap(SubCategory::getId, subCat -> subCat));

            // Iterate over the subcategories in the request
            for (SubCategoryDto subCategoryDto : categoryRequest.getSubCategories()) {
                if (subCategoryDto.getId() != null) {
                    // Update existing subcategory
                    SubCategory existingSubCategory = existingSubCategories.get(subCategoryDto.getId());
                    if (existingSubCategory != null) {
                        existingSubCategory.setName(subCategoryDto.getName());
                    }
                } else {
                    // Add new subcategory
                    SubCategory newSubCategory = new SubCategory();
                    newSubCategory.setName(subCategoryDto.getName());
                    category.addSubCategory(newSubCategory);
                }
            }

            // Remove subcategories that are not in the request
            List<Long> requestedSubCategoryIds = categoryRequest.getSubCategories().stream()
                    .map(SubCategoryDto::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            category.getSubCategories().removeIf(subCat -> !requestedSubCategoryIds.contains(subCat.getId()));
        }

        // Save the updated category (cascade will update subcategories)
        categoryRepo.save(category);


        log.info("✅ Category updated successfully: {}", categoryId);
        return CategoryResponse.builder()
                .status(200)
                .timeStamp(LocalDateTime.now())
                .message("Category updated successfully")
                .build();
    }

    @Override
    @Cacheable(value = "allCategories", condition = "@cacheToggleService.isEnabled()")
    public CategoryResponse getAllCategory() {
        log.info("🔍 [CACHE MISS] Fetching ALL categories from database");

        List<Category> categories = categoryRepo.findAll();
        List<CategoryDto> categoryDtoList = categories.stream()
                .map(categoryMapper::mapCategoryToDtoBasic)
                .collect(Collectors.toList());

        log.info("📦 Retrieved {} categories from database", categories.size());
        CategoryResponse response = CategoryResponse.builder()
                .status(200)
                .categoryList(categoryDtoList)
                .build();
        if (response.isEmpty()) {
            return response;
        }
        return response;
    }

    @Override
    @Cacheable(value = "categoryById", key = "#categoryId",
            condition = "@cacheToggleService.isEnabled()")
    public CategoryResponse getCategoryById(Long categoryId) {
        log.info("🔍 [CACHE MISS] Fetching category by ID: {}", categoryId);

        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        CategoryDto categoryDto = categoryMapper.mapCategoryToDtoBasic(category);

        log.info("📦 Retrieved category: {} from database", category.getName());
        CategoryResponse response = CategoryResponse.builder()
                .status(200)
                .category(categoryDto)
                .build();
        if (response.isEmpty()) {
            return response;
        }
        return response;
    }

    @Override
    @CacheEvict(value = {"allCategories", "categoryById",
            "allSubCategories", "subCategoriesByCategory",
            "categoryWithSubCategories", "categorySearch"
    }, allEntries = true)
    public CategoryResponse deleteCategory(Long categoryId) {
        log.info("🗑️ Deleting category: {}", categoryId);

        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        categoryRepo.delete(category);


        log.info("✅ Category deleted successfully: {}", categoryId);
        return CategoryResponse.builder()
                .status(200)
                .message("Category deleted successfully")
                .build();
    }

    @Override
    @Cacheable(value = "categoryWithSubCategories", key = "#categoryId")
    public CategoryResponse getCategoryByIdWithSubCategories(Long categoryId) {
        log.info("🔍 [CACHE MISS] Fetching category with subcategories: {}", categoryId);

        Category category = categoryRepo.findByIdWithSubCategories(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        CategoryDto categoryDto = categoryMapper.mapCategoryToDtoBasic(category);

        log.info("📦 Retrieved category with {} subcategories: {}",
                category.getSubCategories().size(), category.getName());
        CategoryResponse response = CategoryResponse.builder()
                .status(200)
                .category(categoryDto)
                .build();
        if (response.isEmpty()) {
            return response;
        }
        return response;
    }

    @Override
    @Cacheable(value = "categorySearch", key = "#query")
    public CategoryResponse searchCategories(String query) {
        log.info("🔍 [CACHE MISS] Searching categories with query: {}", query);

        List<Category> categories = categoryRepo.findByNameContainingIgnoreCase(query);
        List<CategoryDto> categoryDtos = categories.stream()
                .map(categoryMapper::mapCategoryToDtoBasic)
                .collect(Collectors.toList());

        log.info("📦 Found {} categories for query: {}", categories.size(), query);
        return CategoryResponse.builder()
                .status(200)
                .categoryList(categoryDtos)
                .build();
    }
}