package com.cuttypaws.response;

import com.cuttypaws.dto.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryResponse {

    private CategoryDto category;
    private List<CategoryDto> categoryList;    private List<SubCategoryDto> subCategoryList;
    private SubCategoryDto subCategory;

    private int status;
    private String message;
    private List<OrderDto> orderList;
    private List<ProductDto> productList;
    private List<UserDto> userList;
    private List<ProductDto> trendingProducts;
    private List<SearchSuggestionDto> suggestions;
    private LocalDateTime timeStamp = LocalDateTime.now();


    public boolean isEmpty() {
        return (productList == null || productList.isEmpty())
                && (subCategoryList == null || subCategoryList.isEmpty())
                && (categoryList == null || categoryList.isEmpty())
                && (orderList == null || orderList.isEmpty())
                && (userList == null || userList.isEmpty())
                && (suggestions == null || suggestions.isEmpty())
                && (trendingProducts == null || trendingProducts.isEmpty());
    }
}
