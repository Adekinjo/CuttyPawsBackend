package com.cuttypaws.response;

import com.cuttypaws.dto.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubCategoryResponse {


    private int status;
    private String message;
    private List<SubCategoryDto> subCategoryList;
    private SubCategoryDto subCategory;
    private LocalDateTime timeStamp;
    private List<ProductDto> productList;
    private List<OrderDto> orderList;
    private List<CategoryDto> categoryList;
    private List<UserDto> userList;
    private List<SearchSuggestionDto> suggestions;
    private List<ProductDto> trendingProducts;
    private OrderItemDto orderItem;
    private List<OrderItemDto> orderItemList;

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
