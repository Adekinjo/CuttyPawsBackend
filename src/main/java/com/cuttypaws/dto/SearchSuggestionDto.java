// SearchSuggestionDto.java
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
    private Long id;
    private String name;
    private String type; // "product", "category", "subcategory"
    private String parentCategory; // Only for subcategory
    private String imageUrl; // For products
}