package com.cuttypaws.dto;

import com.cuttypaws.enums.AdPlanType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateServiceAdSubscriptionRequest {

    @NotNull(message = "Plan type is required")
    private AdPlanType planType;
}