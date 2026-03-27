package com.cuttypaws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestionDto {
    private String id;
    private String name;
    private String type; // product, category, subcategory, service

    private String parentCategory;
    private String imageUrl;

    private String category;
    private String subCategory;

    private String serviceType;
    private String city;
    private String state;
    private String routeId;
}