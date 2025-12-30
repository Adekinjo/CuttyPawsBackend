package com.cuttypaws.controller;

import com.cuttypaws.dto.CategoryDto;
import com.cuttypaws.response.CategoryResponse;
import com.cuttypaws.service.interf.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestPart("category") CategoryDto categoryRequest,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CategoryResponse response = categoryService.createCategory(categoryRequest, imageFile);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{categoryId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @RequestPart("category") CategoryDto categoryRequest,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        CategoryResponse response = categoryService.updateCategory(categoryId, categoryRequest, imageFile);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-all")
    public ResponseEntity<CategoryResponse> getAllCategories(){
        return ResponseEntity.ok(categoryService.getAllCategory());
    }


    @DeleteMapping("/delete/{categoryId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long categoryId){
        return ResponseEntity.ok(categoryService.deleteCategory(categoryId));
    }

    @GetMapping("/get-category-by-id/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long categoryId){
        return ResponseEntity.ok(categoryService.getCategoryById(categoryId));
    }


    //  for sub category
    @GetMapping("/get-with-subcategories/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategoryByIdWithSubCategories(@PathVariable Long categoryId) {
        return ResponseEntity.ok(categoryService.getCategoryByIdWithSubCategories(categoryId));
    }
    @GetMapping("/search")
    public ResponseEntity<CategoryResponse> searchCategories(@RequestParam String query) {
        return ResponseEntity.ok(categoryService.searchCategories(query));
    }
}






