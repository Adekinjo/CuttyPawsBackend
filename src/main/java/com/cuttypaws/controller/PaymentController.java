package com.cuttypaws.controller;

import com.cuttypaws.dto.PaymentOrderRequest;
import com.cuttypaws.dto.PaymentRequest;
import com.cuttypaws.response.OrderResponse;
import com.cuttypaws.response.PaymentResponse;
import com.cuttypaws.service.interf.OrderItemService;
import com.cuttypaws.service.interf.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderItemService orderItemService;

    @PostMapping("/initialize")
    public PaymentResponse initializePayment(@RequestBody PaymentRequest request) {
        log.info("Received payment initialization request for user: {}", request.getUserId());
        return paymentService.initializePayment(request);
    }

    @GetMapping("/verify")
    public PaymentResponse verifyPayment(@RequestParam("reference") String reference) {
        log.info("Received payment verification request for reference: {}", reference);
        return paymentService.verifyPayment(reference);
    }

    @PostMapping("/create-order")
    public OrderResponse createOrderAfterPayment(@RequestBody PaymentOrderRequest request) {
        log.info("Creating order after payment for payment ID: {}", request.getPaymentId());
        return orderItemService.createOrderAfterPayment(request);
    }

    @GetMapping("/{reference}")
    public PaymentResponse getPaymentByReference(@PathVariable String reference) {
        log.info("Getting payment details for reference: {}", reference);
        return paymentService.getPaymentByReference(reference);
    }
}