package com.cuttypaws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailsDto {

    private ProductDto product;

    // same subcategory
    private List<ProductDto> relatedProducts;

    // same category but not already in relatedProducts
    private List<ProductDto> otherRelatedProducts;
}