package com.cuttypaws.controller;

import com.cuttypaws.enums.OrderStatus;
import com.cuttypaws.response.OrderResponse;
import com.cuttypaws.service.interf.OrderItemService;
import com.cuttypaws.service.interf.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderItemController {

    private final OrderItemService orderItemService;

    @PutMapping("/update-item-status/{orderItemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE', 'SELLER')")
    public ResponseEntity<OrderResponse> updateOrderItemStatus(@PathVariable Long orderItemId, @RequestParam String status){
        return ResponseEntity.ok(orderItemService.updateOrderItemStatus(orderItemId, status));
    }


    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE', 'SELLER', 'USER')")
    public ResponseEntity<OrderResponse> filterOrderItems(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)LocalDateTime endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long itemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size

    ){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        OrderStatus orderStatus = status != null ? OrderStatus.valueOf(status.toUpperCase()) : null;

        return ResponseEntity.ok(orderItemService.filterOrderItems(orderStatus, startDate, endDate, itemId, pageable));

    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_PROVIDER', 'CUSTOMER_SUPPORT', 'SELLER', 'USER')")
    public ResponseEntity<OrderResponse> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderItemService.getMyOrders(pageable));
    }

    @GetMapping("/my-order-item/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SELLER', 'CUSTOMER_SUPPORT', 'SERVICE_PROVIDER')")
    public ResponseEntity<OrderResponse> getMyOrderItemById(@PathVariable Long itemId) {
        return ResponseEntity.ok(orderItemService.getMyOrderItemById(itemId));
    }

    @GetMapping("/admin-item/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SUPPORT', 'SELLER')")
    public ResponseEntity<OrderResponse> getOrderItemForAdmin(@PathVariable Long itemId) {
        return ResponseEntity.ok(orderItemService.getOrderItemForAdmin(itemId));
    }

    @GetMapping("/company/{companyId}/orders")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
    public ResponseEntity<OrderResponse> getCompanyProductOrders(
            @PathVariable UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderItemService.getCompanyProductOrders(companyId, pageable));
    }
}
