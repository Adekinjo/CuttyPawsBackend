package com.cuttypaws.dto;

import lombok.Data;

@Data
public class DealRequest {
    private Long productId;
    private int durationHours;
}
