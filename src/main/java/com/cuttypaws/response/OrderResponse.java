package com.cuttypaws.response;

import com.cuttypaws.dto.OrderDto;
import com.cuttypaws.dto.OrderItemDto;
import com.cuttypaws.dto.ProductDto;
import com.cuttypaws.dto.UserDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderResponse {

    private int status;
    private String message;

    private ProductDto product;
    private List<ProductDto> productList;
    private UserDto user;
    private LocalDateTime createdAt;
    private LocalDateTime timeStamp = LocalDateTime.now();
    private List<UserDto> userList;

    private int totalPage;
    private long totalElement;
    private OrderItemDto orderItem;
    private List<OrderItemDto> orderItemList;
    private Long orderId;
    private OrderDto order;
    private List<OrderDto> orderList;
    private Long paymentId;

}
