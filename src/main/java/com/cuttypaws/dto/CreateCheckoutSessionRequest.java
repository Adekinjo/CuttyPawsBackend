package com.cuttypaws.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateCheckoutSessionRequest {
    private List<OrderItemRequest> items;
}