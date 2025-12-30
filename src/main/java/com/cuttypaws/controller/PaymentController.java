package com.cuttypaws.controller;

import com.cuttypaws.dto.PaymentRequest;
import com.cuttypaws.dto.*;
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
        log.info("📍 PRODUCTION: Received payment initialization request for user: {}", request.getUserId());
        return paymentService.initializePayment(request);
    }

    @GetMapping("/verify")
    public PaymentResponse verifyPayment(@RequestParam("reference") String reference) {
        log.info("📍 PRODUCTION: Received payment verification request for reference: {}", reference);
        return paymentService.verifyPayment(reference);
    }

    @PostMapping("/create-order")
    public OrderResponse createOrderAfterPayment(@RequestBody PaymentOrderRequest request) {
        log.info("📍 PRODUCTION: Creating order after payment for payment ID: {}", request.getPaymentId());
        return orderItemService.createOrderAfterPayment(request);
    }

    @GetMapping("/{reference}")
    public PaymentResponse getPaymentByReference(@PathVariable String reference) {
        log.info("📍 PRODUCTION: Getting payment details for reference: {}", reference);
        return paymentService.getPaymentByReference(reference);
    }

    // Paystack callback endpoint
    @GetMapping("/callback")
    public PaymentResponse paymentCallback(@RequestParam("reference") String reference,
                                    @RequestParam("trxref") String trxref) {
        log.info("📍 PRODUCTION: Paystack callback received - Reference: {}, Trxref: {}", reference, trxref);

        try {
            // Verify the payment
            PaymentResponse verificationResponse = paymentService.verifyPayment(reference);

            if (verificationResponse.getStatus() == 200) {
                log.info("✅ PRODUCTION: Payment callback processed successfully for reference: {}", reference);
                return PaymentResponse.builder()
                        .status(200)
                        .message("Payment processed successfully")
                        .reference(reference)
                        .build();
            } else {
                log.error("❌ PRODUCTION: Payment callback failed for reference: {}", reference);
                return PaymentResponse.builder()
                        .status(400)
                        .message("Payment processing failed")
                        .reference(reference)
                        .build();
            }
        } catch (Exception e) {
            log.error("❌ PRODUCTION: Error processing payment callback for reference {}: {}", reference, e.getMessage());
            return PaymentResponse.builder()
                    .status(500)
                    .message("Error processing payment callback")
                    .reference(reference)
                    .build();
        }
    }
}