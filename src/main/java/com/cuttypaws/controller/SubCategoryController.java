package com.cuttypaws.controller;

import com.cuttypaws.dto.SubCategoryDto;
import com.cuttypaws.response.SubCategoryResponse;
import com.cuttypaws.service.interf.SubCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/sub-category")
@RequiredArgsConstructor
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<SubCategoryResponse> createSubCategory(
            @RequestPart("subCategory") SubCategoryDto subCategoryDto,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        return ResponseEntity.ok(subCategoryService.createSubCategory(subCategoryDto, imageFile));
    }

    @PutMapping("/update/{subCategoryId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<SubCategoryResponse> updateSubCategory(
            @PathVariable Long subCategoryId,
            @RequestPart("subCategory") SubCategoryDto subCategoryDto,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        return ResponseEntity.ok(subCategoryService.updateSubCategory(subCategoryId, subCategoryDto, imageFile));
    }

    @GetMapping("get-all-sub-categories")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<SubCategoryResponse> getAllSubCategories(){
        return ResponseEntity.ok(subCategoryService.getAllSubCategories());
    }

    @DeleteMapping("/delete/{subCategoryId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<SubCategoryResponse> deleteSubCategory(@PathVariable Long subCategoryId) {
        return ResponseEntity.ok(subCategoryService.deleteSubCategory(subCategoryId));
    }

    @GetMapping("/get/{subCategoryId}")
    public ResponseEntity<SubCategoryResponse> getSubCategoryById(@PathVariable Long subCategoryId) {
        return ResponseEntity.ok(subCategoryService.getSubCategoryById(subCategoryId));
    }
    // ADD THIS ENDPOINT - This was missing!
    @GetMapping("/get-by-category/{categoryId}")
    public ResponseEntity<SubCategoryResponse> getSubCategoriesByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(subCategoryService.getSubCategoriesByCategory(categoryId));
    }
    @GetMapping("/search")
    public ResponseEntity<SubCategoryResponse> searchSubCategories(@RequestParam String query) {
        return ResponseEntity.ok(subCategoryService.searchSubCategories(query));
    }
}

