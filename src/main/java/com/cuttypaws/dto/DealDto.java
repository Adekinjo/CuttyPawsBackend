package com.cuttypaws.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// DealDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealDto {
    private Long id;
    private ProductDto product;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal discountPercentage;
    private boolean active;
}