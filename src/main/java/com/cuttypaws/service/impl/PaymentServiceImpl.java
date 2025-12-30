package com.cuttypaws.service.impl;

import com.cuttypaws.dto.*;
import com.cuttypaws.entity.*;
import com.cuttypaws.enums.PaymentStatus;
import com.cuttypaws.repository.*;
import com.cuttypaws.response.PaymentResponse;
import com.cuttypaws.service.interf.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PayStackService payStackService;
    private final PaymentRepo paymentRepo;
    private final UserRepo userRepo;

    @Override
    @Transactional
    public PaymentResponse initializePayment(PaymentRequest request) {
        try {
            log.info("🚀 PRODUCTION: Initializing payment for user: {}, amount: {}, email: {}",
                    request.getUserId(), request.getAmount(), request.getEmail());

            // Validate request
            if (request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                return PaymentResponse.builder()
                        .status(400)
                        .message("Invalid amount: " + request.getAmount())
                        .build();
            }

            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return PaymentResponse.builder()
                        .status(400)
                        .message("Email is required")
                        .build();
            }

            User user = userRepo.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

            // Initialize payment with Paystack
            Map<String, String> paymentInitResponse = payStackService.initializePayment(
                    request.getAmount(),
                    request.getEmail(),
                    request.getCurrency()
            );

            String authorizationUrl = paymentInitResponse.get("authorizationUrl");
            String paystackReference = paymentInitResponse.get("reference");

            // Create payment record
            Payment payment = new Payment();
            payment.setAmount(request.getAmount());
            payment.setEmail(request.getEmail());
            payment.setCurrency(request.getCurrency());
            payment.setMethod(request.getMethod());
            payment.setReference(paystackReference);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setUser(user);
            payment.setTransactionId(UUID.randomUUID().toString());

            Payment savedPayment = paymentRepo.save(payment);

            log.info("✅ PRODUCTION: Payment initialized successfully. Payment ID: {}, Reference: {}",
                    savedPayment.getId(), paystackReference);

            return PaymentResponse.builder()
                    .status(200)
                    .message("Payment initialized successfully")
                    .authorizationUrl(authorizationUrl)
                    .reference(paystackReference)
                    .paymentId(savedPayment.getId())
                    .build();

        } catch (Exception e) {
            log.error("❌ PRODUCTION: Error initializing payment: {}", e.getMessage(), e);
            return PaymentResponse.builder()
                    .status(500)
                    .message("Error initializing payment: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(String reference) {
        try {
            log.info("🔍 PRODUCTION: Verifying payment with reference: {}", reference);

            // First, check if payment exists in our database
            Optional<Payment> paymentOptional = paymentRepo.findByReference(reference);
            if (paymentOptional.isEmpty()) {
                log.error("❌ PRODUCTION: Payment not found in database with reference: {}", reference);
                return PaymentResponse.builder()
                        .status(404)
                        .message("Payment not found with reference: " + reference)
                        .build();
            }

            Payment payment = paymentOptional.get();
            log.info("📋 PRODUCTION: Payment found in database. ID: {}, Status: {}", payment.getId(), payment.getStatus());

            // If payment is already successful, return success
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                log.info("✅ PRODUCTION: Payment already verified as SUCCESS");
                return PaymentResponse.builder()
                        .status(200)
                        .message("Payment already verified successfully")
                        .paymentId(payment.getId())
                        .userId(payment.getUser().getId())
                        .amount(payment.getAmount())
                        .reference(payment.getReference())
                        .build();
            }

            // Verify with Paystack
            boolean isSuccessful = payStackService.verifyPayment(reference);

            if (isSuccessful) {
                payment.setStatus(PaymentStatus.SUCCESS);
                paymentRepo.save(payment);

                log.info("🎉 PRODUCTION: Payment verified successfully. Payment ID: {}, Reference: {}", payment.getId(), reference);

                return PaymentResponse.builder()
                        .status(200)
                        .message("Payment verified successfully")
                        .paymentId(payment.getId())
                        .userId(payment.getUser().getId())
                        .amount(payment.getAmount())
                        .reference(payment.getReference())
                        .build();
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepo.save(payment);

                log.warn("⚠️ PRODUCTION: Payment verification failed. Payment ID: {}, Reference: {}", payment.getId(), reference);

                return PaymentResponse.builder()
                        .status(400)
                        .message("Payment verification failed")
                        .paymentId(payment.getId())
                        .reference(reference)
                        .build();
            }
        } catch (Exception e) {
            log.error("❌ PRODUCTION: Error verifying payment with reference {}: {}", reference, e.getMessage(), e);
            return PaymentResponse.builder()
                    .status(500)
                    .message("Error verifying payment: " + e.getMessage())
                    .reference(reference)
                    .build();
        }
    }

    // Get payment by reference - FIXED VERSION
    public PaymentResponse getPaymentByReference(String reference) {
        try {
            Optional<Payment> paymentOptional = paymentRepo.findByReference(reference);
            if (paymentOptional.isEmpty()) {
                return PaymentResponse.builder()
                        .status(404)
                        .message("Payment not found")
                        .build();
            }

            Payment payment = paymentOptional.get();

            return PaymentResponse.builder()
                    .status(200)
                    .message("Payment details retrieved")
                    .paymentId(payment.getId())
                    .reference(payment.getReference())
                    .amount(payment.getAmount())
                    .userId(payment.getUser().getId())
                    .build();

        } catch (Exception e) {
            return PaymentResponse.builder()
                    .status(500)
                    .message("Error retrieving payment: " + e.getMessage())
                    .build();
        }
    }
}