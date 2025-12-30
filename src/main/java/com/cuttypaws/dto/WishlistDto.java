package com.cuttypaws.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@RequiredArgsConstructor
public class WishlistDto {
    private Long id;
    private Long userId;
    private Long productId;
    private BigDecimal price;
    private String productName; // Add
    private String productImage; // Add

}
