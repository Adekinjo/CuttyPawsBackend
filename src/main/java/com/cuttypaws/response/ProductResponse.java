package com.cuttypaws.response;

import com.cuttypaws.dto.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class ProductResponse {

    private int status;
    private String message;
    private LocalDateTime timeStamp;
    private List<ProductDto> trendingProducts;

    private List<UserDto> userList;

    private CategoryDto category;
    private List<CategoryDto> categoryList;


    private ProductDto product;
    private List<ProductDto> productList;

    private List<SubCategoryDto> subCategoryList;
    private SubCategoryDto subCategory;
    private List<OrderDto> orderList;
    private List<SearchSuggestionDto> suggestions;


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
