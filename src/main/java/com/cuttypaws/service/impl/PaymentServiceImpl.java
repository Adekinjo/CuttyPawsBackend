package com.cuttypaws.service.impl;

import com.cuttypaws.dto.PaymentSheetRequest;
import com.cuttypaws.entity.*;
import com.cuttypaws.enums.PaymentProvider;
import com.cuttypaws.enums.PaymentPurpose;
import com.cuttypaws.enums.PaymentStatus;
import com.cuttypaws.repository.*;
import com.cuttypaws.response.PaymentSheetResponse;
import com.cuttypaws.service.interf.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepo paymentRepo;
    private final UserRepo userRepo;
    private final ServiceAdSubscriptionRepo serviceAdSubscriptionRepo;
    private final ServiceBookingRepo serviceBookingRepo;
    private final StripePaymentSheetService stripePaymentSheetService;
    private final CheckoutSessionRepo checkoutSessionRepo;

    @Override
    @Transactional
    public PaymentSheetResponse initializePaymentSheet(PaymentSheetRequest request) {
        try {
            PaymentPurpose purpose = request.getPaymentPurpose() == null
                    ? PaymentPurpose.ORDER
                    : request.getPaymentPurpose();

            String platform = request.getPlatform() == null || request.getPlatform().isBlank()
                    ? "WEB"
                    : request.getPlatform().trim().toUpperCase();

            Payment payment = new Payment();
            payment.setProvider(PaymentProvider.STRIPE);
            payment.setMethod("PAYMENT_SHEET");
            payment.setReference(buildReference(purpose));
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentPurpose(purpose);
            payment.setTransactionId(UUID.randomUUID().toString());

            User user;

            switch (purpose) {
                case ORDER -> {
                    if (request.getCheckoutSessionId() == null) {
                        return PaymentSheetResponse.builder()
                                .status(400)
                                .message("checkoutSessionId is required for ORDER payment")
                                .build();
                    }

                    CheckoutSession checkoutSession = checkoutSessionRepo.findById(request.getCheckoutSessionId())
                            .orElseThrow(() -> new RuntimeException("Checkout session not found"));

                    if (checkoutSession.getExpiresAt() != null &&
                            checkoutSession.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                        return PaymentSheetResponse.builder()
                                .status(400)
                                .message("Checkout session has expired")
                                .build();
                    }

                    user = checkoutSession.getUser();

                    payment.setAmount(checkoutSession.getTotalAmount());
                    payment.setEmail(user.getEmail());
                    payment.setCurrency(checkoutSession.getCurrency());
                    payment.setUser(user);
                    payment.setCheckoutSession(checkoutSession);
                }

                case SERVICE_AD -> {
                    if (request.getServiceAdSubscriptionId() == null) {
                        return PaymentSheetResponse.builder()
                                .status(400)
                                .message("serviceAdSubscriptionId is required for SERVICE_AD")
                                .build();
                    }

                    ServiceAdSubscription subscription = serviceAdSubscriptionRepo.findById(request.getServiceAdSubscriptionId())
                            .orElseThrow(() -> new RuntimeException("Service ad subscription not found"));

                    user = subscription.getUser();

                    payment.setAmount(subscription.getAmount().setScale(2, java.math.RoundingMode.HALF_UP));
                    payment.setEmail(user.getEmail());
                    payment.setCurrency("USD");
                    payment.setUser(user);
                    payment.setServiceAdSubscription(subscription);
                }

                case SERVICE_BOOKING -> {
                    if (request.getServiceBookingId() == null) {
                        return PaymentSheetResponse.builder()
                                .status(400)
                                .message("serviceBookingId is required for SERVICE_BOOKING")
                                .build();
                    }

                    ServiceBooking booking = serviceBookingRepo.findById(request.getServiceBookingId())
                            .orElseThrow(() -> new RuntimeException("Service booking not found"));

                    user = booking.getCustomer();

                    payment.setAmount(booking.getAmount().setScale(2, java.math.RoundingMode.HALF_UP));
                    payment.setEmail(user.getEmail());
                    payment.setCurrency("USD");
                    payment.setUser(user);
                    payment.setServiceBooking(booking);
                }

                default -> {
                    return PaymentSheetResponse.builder()
                            .status(400)
                            .message("Unsupported payment purpose")
                            .build();
                }
            }

            Payment savedPayment = paymentRepo.save(payment);

            StripePaymentSheetService.StripePaymentSheetInitResult stripeResult;

            if ("MOBILE".equals(platform)) {
                stripeResult = stripePaymentSheetService.createMobilePayment(savedPayment, savedPayment.getUser());

                savedPayment.setStripeCustomerId(stripeResult.getCustomerId());

                if (savedPayment.getUser().getStripeCustomerId() == null ||
                        savedPayment.getUser().getStripeCustomerId().isBlank()) {
                    savedPayment.getUser().setStripeCustomerId(stripeResult.getCustomerId());
                    userRepo.save(savedPayment.getUser());
                }
            } else {
                stripeResult = stripePaymentSheetService.createWebPayment(savedPayment);
            }

            savedPayment.setPaymentIntentId(stripeResult.getPaymentIntentId());
            paymentRepo.save(savedPayment);

            log.info("Saved paymentIntentId={} for paymentId={} reference={}",
                    savedPayment.getPaymentIntentId(),
                    savedPayment.getId(),
                    savedPayment.getReference());

            return PaymentSheetResponse.builder()
                    .status(200)
                    .message("Payment initialized successfully")
                    .paymentIntentId(stripeResult.getPaymentIntentId())
                    .paymentIntentClientSecret(stripeResult.getPaymentIntentClientSecret())
                    .customerId(stripeResult.getCustomerId())
                    .customerEphemeralKeySecret(stripeResult.getCustomerEphemeralKeySecret())
                    .publishableKey(stripeResult.getPublishableKey())
                    .paymentId(savedPayment.getId())
                    .reference(savedPayment.getReference())
                    .amount(savedPayment.getAmount())
                    .currency(savedPayment.getCurrency())
                    .userId(savedPayment.getUser().getId())
                    .paymentPurpose(savedPayment.getPaymentPurpose())
                    .paymentStatus(savedPayment.getStatus().name())
                    .build();

        } catch (Exception e) {
            log.error("Error initializing payment", e);
            return PaymentSheetResponse.builder()
                    .status(500)
                    .message("Error initializing payment: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentSheetResponse getPaymentStatus(String reference) {
        try {
            Optional<Payment> paymentOptional = paymentRepo.findByReference(reference);

            if (paymentOptional.isEmpty()) {
                return PaymentSheetResponse.builder()
                        .status(404)
                        .message("Payment not found")
                        .build();
            }

            Payment payment = paymentOptional.get();
            log.info("Payment status check for reference={} returned status={}",
                    payment.getReference(), payment.getStatus());

            return PaymentSheetResponse.builder()
                    .status(200)
                    .message("Payment retrieved successfully")
                    .paymentId(payment.getId())
                    .reference(payment.getReference())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .userId(payment.getUser().getId())
                    .paymentPurpose(payment.getPaymentPurpose())
                    .paymentStatus(payment.getStatus().name())
                    .paymentIntentId(payment.getPaymentIntentId())
                    .customerId(payment.getStripeCustomerId())
                    .build();

        } catch (Exception e) {
            log.error("Error getting payment status", e);
            return PaymentSheetResponse.builder()
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