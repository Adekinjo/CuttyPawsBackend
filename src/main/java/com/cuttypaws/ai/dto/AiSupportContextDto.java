package com.cuttypaws.ai.dto;

import com.cuttypaws.dto.ProductDto;
import com.cuttypaws.dto.ServiceProfileDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiSupportContextDto {
    private List<ProductDto> products;
    private List<ServiceProfileDto> services;
    private List<AiOrderSummaryDto> orders;
    private List<AiPaymentSummaryDto> payments;
}