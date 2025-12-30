package com.cuttypaws.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PaymentOrderRequest {
    private Long paymentId;
    private List<OrderItemRequest> items;
    private BigDecimal totalPrice;
}