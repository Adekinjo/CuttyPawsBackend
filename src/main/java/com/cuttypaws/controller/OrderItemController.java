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
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_CUSTOMER_SERVICE', 'ROLE_COMPANY')")
    public ResponseEntity<OrderResponse> updateOrderItemStatus(@PathVariable Long orderItemId, @RequestParam String status){
        return ResponseEntity.ok(orderItemService.updateOrderItemStatus(orderItemId, status));
    }


    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_CUSTOMER_SERVICE', 'ROLE_COMPANY', 'ROLE_USER')")
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
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_CUSTOMER_SERVICE', 'ROLE_COMPANY', 'ROLE_USER')")
    public ResponseEntity<OrderResponse> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderItemService.getMyOrders(pageable));
    }


    @GetMapping("/company/{companyId}/orders")
    @PreAuthorize("hasAuthority('ROLE_COMPANY')")
    public ResponseEntity<OrderResponse> getCompanyProductOrders(
            @PathVariable UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderItemService.getCompanyProductOrders(companyId, pageable));
    }
}
