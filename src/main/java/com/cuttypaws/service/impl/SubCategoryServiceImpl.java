package com.cuttypaws.service.impl;

import com.cuttypaws.cache.CacheMonitorService;
import com.cuttypaws.dto.*;
import com.cuttypaws.entity.*;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.mapper.SubCategoryMapper;
import com.cuttypaws.repository.*;
import com.cuttypaws.response.SubCategoryResponse;
import com.cuttypaws.service.AwsS3Service;
import com.cuttypaws.service.interf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
// REMOVED @CacheConfig - it causes conflicts
public class SubCategoryServiceImpl implements SubCategoryService {

    private final SubCategoryRepo subCategoryRepo;
    private final CategoryRepo categoryRepo;
    private final SubCategoryMapper subCategoryMapper;
    private final AwsS3Service awsS3Service;
    private final CacheMonitorService cacheMonitorService;

    @Override
    @CacheEvict(value = {"allCategories", "categoryById", "subCategoryById"}, allEntries = true)
    public SubCategoryResponse createSubCategory(SubCategoryDto subCategoryRequest, MultipartFile imageFile) {
        //log.info("🆕 Creating new subcategory: {}", subCategoryRequest.getName());

        Category category = categoryRepo.findById(subCategoryRequest.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        SubCategory subCategory = new SubCategory();
        subCategory.setName(subCategoryRequest.getName());
        subCategory.setCategory(category);

        // Upload image to S3 if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageUrl = awsS3Service.saveImageToS3(imageFile);
                subCategory.setImageUrl(imageUrl);
                //log.info("📸 Subcategory image uploaded successfully: {}", imageUrl);
            } catch (Exception e) {
                //log.error("❌ Failed to upload subcategory image: {}", e.getMessage());
                throw new RuntimeException("Failed to upload subcategory image: " + e.getMessage());
            }
        }

        subCategoryRepo.save(subCategory);


        //log.info("✅ SubCategory created successfully: {}", subCategoryRequest.getName());
        return SubCategoryResponse.builder()
                .status(200)
                .timeStamp(LocalDateTime.now())
                .message("SubCategory created successfully")
                .build();
    }

    @Override
    @CacheEvict(value = {"allCategories", "categoryById", "subCategoryById"}, allEntries = true)
    public SubCategoryResponse updateSubCategory(Long subCategoryId, SubCategoryDto subCategoryRequest, MultipartFile imageFile) {
        //log.info("🔄 Updating subcategory: {}", subCategoryId);

        SubCategory subCategory = subCategoryRepo.findById(subCategoryId)
                .orElseThrow(() -> new NotFoundException("SubCategory not found"));

        //log.info("📝 Current subcategory name: {}, image: {}", subCategory.getName(), subCategory.getImageUrl());

        subCategory.setName(subCategoryRequest.getName());

        // Update image if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                //log.info("📤 Uploading new image for subcategory: {}", subCategoryId);
                String imageUrl = awsS3Service.saveImageToS3(imageFile);
                subCategory.setImageUrl(imageUrl);
                //log.info("📸 Subcategory image updated successfully: {}", imageUrl);
            } catch (Exception e) {
                //log.error("❌ Failed to update subcategory image: {}", e.getMessage());
                throw new RuntimeException("Failed to update subcategory image: " + e.getMessage());
            }
        } else {
            //log.info("📷 No new image provided, keeping existing image: {}", subCategory.getImageUrl());
        }

        // Save the subcategory
        SubCategory savedSubCategory = subCategoryRepo.save(subCategory);
        //log.info("💾 Subcategory saved with name: {}, image: {}", savedSubCategory.getName(), savedSubCategory.getImageUrl());

        //log.info("✅ SubCategory updated successfully: {}", subCategoryId);
        return SubCategoryResponse.builder()
                .status(200)
                .timeStamp(LocalDateTime.now())
                .message("SubCategory updated successfully")
                .build();
    }

    @Override
    @CacheEvict(value = {"allCategories", "categoryById", "subCategoryById", "subCategoriesByCategory"}, allEntries = true)
    public SubCategoryResponse deleteSubCategory(Long subCategoryId) {
        //log.info("🗑️ Deleting subcategory: {}", subCategoryId);

        SubCategory subCategory = subCategoryRepo.findById(subCategoryId)
                .orElseThrow(() -> new NotFoundException("SubCategory not found"));

        subCategoryRepo.delete(subCategory);

        //log.info("✅ SubCategory deleted successfully: {}", subCategoryId);
        return SubCategoryResponse.builder()
                .status(200)
                .timeStamp(LocalDateTime.now())
                .message("SubCategory deleted successfully")
                .build();
    }

    @Override
    @Cacheable(value = "subCategoryById", key = "#subCategoryId")
    public SubCategoryResponse getSubCategoryById(Long subCategoryId) {
        log.info("🔍 [CACHE MISS] Fetching subcategory by ID: {}", subCategoryId);

        SubCategory subCategory = subCategoryRepo.findById(subCategoryId)
                .orElseThrow(() -> new NotFoundException("SubCategory not found"));

        SubCategoryDto subCategoryDto = subCategoryMapper.mapSubCategoryToDto(subCategory);

        //log.info("📦 Retrieved subcategory: {} from database", subCategory.getName());
        SubCategoryResponse response = SubCategoryResponse.builder()
                .status(200)
                .subCategory(subCategoryDto)
                .build();
        if (response.isEmpty()) {
            return response;
        }
        return response;
    }

    @Override
    @Cacheable(value = "allSubCategories")
    public SubCategoryResponse getAllSubCategories() {
        log.info("🔍 [CACHE MISS] Fetching ALL subcategories from database");

        List<SubCategory> subCategories = subCategoryRepo.findAll();
        List<SubCategoryDto> subCategoryDtos = subCategories.stream()
                .map(subCategoryMapper::mapSubCategoryToDto)
                .collect(Collectors.toList());

        //log.info("📦 Retrieved {} subcategories from database", subCategories.size());
        SubCategoryResponse response = SubCategoryResponse.builder()
                .status(200)
                .subCategoryList(subCategoryDtos)
                .build();
        if (response.isEmpty()) {
            return response;
        }
        return response;
    }

    @Override
    @Cacheable(value = "subCategorySearch", key = "#query")
    public SubCategoryResponse searchSubCategories(String query) {
        log.info("🔍 [CACHE MISS] Searching subcategories with query: {}", query);

        List<SubCategory> subCategories = subCategoryRepo.findByNameContainingIgnoreCase(query);
        List<SubCategoryDto> subCategoryDtos = subCategories.stream()
                .map(subCategoryMapper::mapSubCategoryToDto)
                .collect(Collectors.toList());

        log.info("📦 Found {} subcategories for query: {}", subCategories.size(), query);
        return SubCategoryResponse.builder()
                .status(200)
                .subCategoryList(subCategoryDtos)
                .build();
    }

    // Add this method to your SubCategoryService interface
    @Cacheable(value = "subCategoriesByCategory", key = "#categoryId")
    public SubCategoryResponse getSubCategoriesByCategory(Long categoryId) {
        log.info("🔍 [CACHE MISS] Fetching subcategories for category: {}", categoryId);

        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        List<SubCategoryDto> subCategoryDtos = category.getSubCategories().stream()
                .map(subCategoryMapper::mapSubCategoryToDto)
                .collect(Collectors.toList());

        //log.info("📦 Retrieved {} subcategories for category: {}", subCategoryDtos.size(), categoryId);
        SubCategoryResponse response = SubCategoryResponse.builder()
                .status(200)
                .subCategoryList(subCategoryDtos)
                .build();
        if (response.isEmpty()) {
            return response;
        }
        return response;
    }
}