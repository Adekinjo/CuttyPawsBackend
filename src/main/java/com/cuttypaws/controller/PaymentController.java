package com.cuttypaws.controller;

import com.cuttypaws.dto.PaymentOrderRequest;
import com.cuttypaws.dto.PaymentSheetRequest;
import com.cuttypaws.response.OrderResponse;
import com.cuttypaws.response.PaymentSheetResponse;
import com.cuttypaws.service.interf.OrderItemService;
import com.cuttypaws.service.interf.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderItemService orderItemService;

    @PostMapping("/payment-sheet/initialize")
    public PaymentSheetResponse initializePaymentSheet(@RequestBody PaymentSheetRequest request) {
        return paymentService.initializePaymentSheet(request);
    }

    @GetMapping("/status")
    public PaymentSheetResponse getPaymentStatus(@RequestParam String reference) {
        return paymentService.getPaymentStatus(reference);
    }

    @PostMapping("/create-order")
    public OrderResponse createOrderAfterPayment(@RequestBody PaymentOrderRequest request) {
        return orderItemService.createOrderAfterPayment(request);
    }
}