package com.cuttypaws.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@RequiredArgsConstructor
public class WishlistDto {
    private Long id;
    private UUID userId;
    private Long productId;
    private BigDecimal price;
    private String productName; // Add
    private String productImage; // Add

}
