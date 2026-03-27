package com.cuttypaws.service.impl;

import com.cuttypaws.dto.PaymentRequest;
import com.cuttypaws.entity.Payment;
import com.cuttypaws.entity.User;
import com.cuttypaws.enums.PaymentProvider;
import com.cuttypaws.enums.PaymentPurpose;
import com.cuttypaws.enums.PaymentStatus;
import com.cuttypaws.repository.PaymentRepo;
import com.cuttypaws.repository.UserRepo;
import com.cuttypaws.response.PaymentResponse;
import com.cuttypaws.service.interf.PaymentService;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepo paymentRepo;
    private final UserRepo userRepo;
    private final StripeService stripeService;

    @Override
    @Transactional
    public PaymentResponse initializePayment(PaymentRequest request) {
        try {
            if (request.getAmount() == null || request.getAmount().signum() <= 0) {
                return PaymentResponse.builder()
                        .status(400)
                        .message("Invalid amount")
                        .build();
            }

            if (request.getEmail() == null || request.getEmail().isBlank()) {
                return PaymentResponse.builder()
                        .status(400)
                        .message("Email is required")
                        .build();
            }

            User user = userRepo.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            PaymentPurpose purpose = request.getPaymentPurpose() == null
                    ? PaymentPurpose.ORDER
                    : request.getPaymentPurpose();

            Payment payment = new Payment();
            payment.setAmount(request.getAmount());
            payment.setEmail(request.getEmail());
            payment.setCurrency(
                    request.getCurrency() == null || request.getCurrency().isBlank()
                            ? "USD"
                            : request.getCurrency().toUpperCase()
            );
            payment.setProvider(PaymentProvider.STRIPE);
            payment.setMethod("STRIPE");
            payment.setReference(buildReference(purpose));
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentPurpose(purpose);
            payment.setUser(user);
            payment.setTransactionId(UUID.randomUUID().toString());

            Payment savedPayment = paymentRepo.save(payment);

            Session session = stripeService.createOrderCheckoutSession(savedPayment);

            savedPayment.setCheckoutSessionId(session.getId());
            savedPayment.setPaymentUrl(session.getUrl());
            paymentRepo.save(savedPayment);

            return PaymentResponse.builder()
                    .status(200)
                    .message("Payment initialized successfully")
                    .authorizationUrl(session.getUrl())
                    .reference(savedPayment.getReference())
                    .paymentId(savedPayment.getId())
                    .paymentPurpose(savedPayment.getPaymentPurpose())
                    .build();

        } catch (Exception e) {
            log.error("Error initializing Stripe payment", e);
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
            Optional<Payment> paymentOptional = paymentRepo.findByReference(reference);
            if (paymentOptional.isEmpty()) {
                return PaymentResponse.builder()
                        .status(404)
                        .message("Payment not found")
                        .build();
            }

            Payment payment = paymentOptional.get();

            if (payment.getStatus() == PaymentStatus.PAID) {
                return PaymentResponse.builder()
                        .status(200)
                        .message("Payment already verified successfully")
                        .paymentId(payment.getId())
                        .userId(payment.getUser().getId())
                        .amount(payment.getAmount())
                        .reference(payment.getReference())
                        .paymentPurpose(payment.getPaymentPurpose())
                        .build();
            }

            if (payment.getCheckoutSessionId() == null) {
                return PaymentResponse.builder()
                        .status(400)
                        .message("Checkout session not found for payment")
                        .build();
            }

            Session session = stripeService.retrieveCheckoutSession(payment.getCheckoutSessionId());

            if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaymentIntentId(session.getPaymentIntent());
                paymentRepo.save(payment);

                return PaymentResponse.builder()
                        .status(200)
                        .message("Payment verified successfully")
                        .paymentId(payment.getId())
                        .userId(payment.getUser().getId())
                        .amount(payment.getAmount())
                        .reference(payment.getReference())
                        .paymentPurpose(payment.getPaymentPurpose())
                        .build();
            }

            if ("unpaid".equalsIgnoreCase(session.getPaymentStatus())) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepo.save(payment);
            }

            return PaymentResponse.builder()
                    .status(400)
                    .message("Payment verification failed")
                    .paymentId(payment.getId())
                    .reference(reference)
                    .paymentPurpose(payment.getPaymentPurpose())
                    .build();

        } catch (Exception e) {
            log.error("Error verifying Stripe payment", e);
            return PaymentResponse.builder()
                    .status(500)
                    .message("Error verifying payment: " + e.getMessage())
                    .reference(reference)
                    .build();
        }
    }

    @Override
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
                    .message("Payment retrieved successfully")
                    .paymentId(payment.getId())
                    .userId(payment.getUser().getId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .reference(payment.getReference())
                    .paymentPurpose(payment.getPaymentPurpose())
                    .authorizationUrl(payment.getPaymentUrl())
                    .build();

        } catch (Exception e) {
            log.error("Error getting payment by reference", e);
            return PaymentResponse.builder()
                    .status(500)
                    .message("Error retrieving payment: " + e.getMessage())
                    .build();
        }
    }

    private String buildReference(PaymentPurpose purpose) {
        String prefix = switch (purpose) {
            case ORDER -> "ORD-";
            case SERVICE_AD -> "AD-";
            case SERVICE_BOOKING -> "SBK-";
        };

        return prefix + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase();
    }
}