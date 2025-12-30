package com.cuttypaws.dto;


import com.cuttypaws.entity.Payment;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderRequest {

    private BigDecimal totalPrice;
    private List<OrderItemRequest> items;

    public Long userId;

    private String paymentMethod;

    private Payment paymentInfo;

}
