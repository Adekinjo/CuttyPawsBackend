package com.cuttypaws.service.impl;

import com.cuttypaws.dto.PaymentSheetRequest;
import com.cuttypaws.entity.Payment;
import com.cuttypaws.entity.ServiceAdSubscription;
import com.cuttypaws.entity.ServiceBooking;
import com.cuttypaws.entity.User;
import com.cuttypaws.enums.PaymentProvider;
import com.cuttypaws.enums.PaymentPurpose;
import com.cuttypaws.enums.PaymentStatus;
import com.cuttypaws.repository.PaymentRepo;
import com.cuttypaws.repository.ServiceAdSubscriptionRepo;
import com.cuttypaws.repository.ServiceBookingRepo;
import com.cuttypaws.repository.UserRepo;
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

    @Override
    @Transactional
    public PaymentSheetResponse initializePaymentSheet(PaymentSheetRequest request) {
        try {
            if (request.getAmount() == null || request.getAmount().signum() <= 0) {
                return PaymentSheetResponse.builder()
                        .status(400)
                        .message("Invalid amount")
                        .build();
            }

            if (request.getEmail() == null || request.getEmail().isBlank()) {
                return PaymentSheetResponse.builder()
                        .status(400)
                        .message("Email is required")
                        .build();
            }

            if (request.getUserId() == null) {
                return PaymentSheetResponse.builder()
                        .status(400)
                        .message("User ID is required")
                        .build();
            }

            User user = userRepo.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            PaymentPurpose purpose = request.getPaymentPurpose() == null
                    ? PaymentPurpose.ORDER
                    : request.getPaymentPurpose();

            String platform = request.getPlatform() == null
                    ? "WEB"
                    : request.getPlatform().trim().toUpperCase();

            Payment payment = new Payment();
            payment.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
            payment.setEmail(request.getEmail());
            payment.setCurrency(
                    request.getCurrency() == null || request.getCurrency().isBlank()
                            ? "USD"
                            : request.getCurrency().toUpperCase()
            );
            payment.setProvider(PaymentProvider.STRIPE);
            payment.setMethod("PAYMENT_SHEET");
            payment.setReference(buildReference(purpose));
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentPurpose(purpose);
            payment.setUser(user);
            payment.setTransactionId(UUID.randomUUID().toString());

            if (purpose == PaymentPurpose.SERVICE_AD) {
                if (request.getServiceAdSubscriptionId() == null) {
                    return PaymentSheetResponse.builder()
                            .status(400)
                            .message("serviceAdSubscriptionId is required for SERVICE_AD")
                            .build();
                }

                ServiceAdSubscription subscription = serviceAdSubscriptionRepo
                        .findById(request.getServiceAdSubscriptionId())
                        .orElseThrow(() -> new RuntimeException("Service ad subscription not found"));

                payment.setServiceAdSubscription(subscription);
            }

            if (purpose == PaymentPurpose.SERVICE_BOOKING) {
                if (request.getServiceBookingId() == null) {
                    return PaymentSheetResponse.builder()
                            .status(400)
                            .message("serviceBookingId is required for SERVICE_BOOKING")
                            .build();
                }

                ServiceBooking booking = serviceBookingRepo
                        .findById(request.getServiceBookingId())
                        .orElseThrow(() -> new RuntimeException("Service booking not found"));

                payment.setServiceBooking(booking);
            }

            Payment savedPayment = paymentRepo.save(payment);

            StripePaymentSheetService.StripePaymentSheetInitResult stripeResult;

            if ("MOBILE".equals(platform)) {
                stripeResult = stripePaymentSheetService.createMobilePayment(savedPayment, user);

                savedPayment.setStripeCustomerId(stripeResult.getCustomerId());

                if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isBlank()) {
                    user.setStripeCustomerId(stripeResult.getCustomerId());
                    userRepo.save(user);
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