package com.cuttypaws.service.interf;

import com.cuttypaws.dto.*;

import com.cuttypaws.enums.OrderStatus;
import com.cuttypaws.response.OrderResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderItemService {
    OrderResponse createOrderAfterPayment(PaymentOrderRequest request);

    OrderResponse updateOrderItemStatus(Long orderItemId, String status);
    OrderResponse filterOrderItems(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate, Long itemId, Pageable pageable);

    OrderResponse getCompanyProductOrders(UUID companyId, Pageable pageable);

    OrderResponse getMyOrders(Pageable pageable);

}

