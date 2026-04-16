package com.cuttypaws.response;

import com.cuttypaws.dto.OrderItemDto;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CheckoutSessionResponse {
    private int status;
    private String message;
    private Long checkoutSessionId;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItemDto> items;
}