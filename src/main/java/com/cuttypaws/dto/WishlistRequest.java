package com.cuttypaws.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class WishlistRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Product ID is required")
    private Long productId;
}
