package com.cuttypaws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductImageDto {
    private Long id;
    private String imageUrl;


}