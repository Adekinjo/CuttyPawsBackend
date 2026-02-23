package com.cuttypaws.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder  // new
public class ProductDto {

    private Long id;
    private String name;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    private BigDecimal oldPrice; // Add this field
    private BigDecimal newPrice; // Add this field
    private String thumbnailImageUrl;
    private List<String> imageUrls;

    private Long categoryId; // Only include the ID, not the entire Category entity
    private Long subCategoryId;

    private List<ColorDto> colors;

    private  String category;
    private String subCategory;

    private List<String> sizes;

    //private Boolean isFeatured;
    private Integer viewCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastViewedDate;

    private Integer likes;
    private Integer stock;


    private UUID userId;
    private String companyName;


}


