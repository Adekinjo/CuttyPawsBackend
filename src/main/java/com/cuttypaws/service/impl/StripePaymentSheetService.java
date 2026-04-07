package com.cuttypaws.service.impl;

import com.cuttypaws.entity.Payment;
import com.cuttypaws.entity.User;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.EphemeralKey;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.EphemeralKeyCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripePaymentSheetService {

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    @Value("${stripe.mobile.ephemeral-key-api-version}")
    private String mobileEphemeralKeyApiVersion;

    public StripePaymentSheetInitResult createWebPayment(Payment payment) throws StripeException {
        PaymentIntent paymentIntent = createPaymentIntent(payment, null);

        return StripePaymentSheetInitResult.builder()
                .paymentIntentId(paymentIntent.getId())
                .paymentIntentClientSecret(paymentIntent.getClientSecret())
                .publishableKey(stripePublishableKey)
                .build();
    }

    public StripePaymentSheetInitResult createMobilePayment(Payment payment, User user) throws StripeException {
        String customerId = getOrCreateStripeCustomer(user);

        PaymentIntent paymentIntent = createPaymentIntent(payment, customerId);
        String ephemeralKeySecret = createEphemeralKey(customerId);

        return StripePaymentSheetInitResult.builder()
                .paymentIntentId(paymentIntent.getId())
                .paymentIntentClientSecret(paymentIntent.getClientSecret())
                .customerId(customerId)
                .customerEphemeralKeySecret(ephemeralKeySecret)
                .publishableKey(stripePublishableKey)
                .build();
    }

    private String getOrCreateStripeCustomer(User user) throws StripeException {
        if (user.getStripeCustomerId() != null && !user.getStripeCustomerId().isBlank()) {
            return user.getStripeCustomerId();
        }

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getName())
                .putMetadata("userId", user.getId().toString())
                .build();

        Customer customer = Customer.create(params);
        return customer.getId();
    }

    private PaymentIntent createPaymentIntent(Payment payment, String customerId) throws StripeException {
        BigDecimal normalizedAmount = payment.getAmount().setScale(2, RoundingMode.HALF_UP);
        long amountInCents = normalizedAmount.movePointRight(2).longValueExact();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("paymentId", String.valueOf(payment.getId()));
        metadata.put("reference", payment.getReference());
        metadata.put("paymentPurpose", payment.getPaymentPurpose().name());
        metadata.put("userId", payment.getUser().getId().toString());

        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(payment.getCurrency().toLowerCase())
                .setReceiptEmail(payment.getEmail())
                .putAllMetadata(metadata)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                );

        if (customerId != null && !customerId.isBlank()) {
            builder.setCustomer(customerId);
        }

        return PaymentIntent.create(builder.build());
    }

    private String createEphemeralKey(String customerId) throws StripeException {
        EphemeralKeyCreateParams params = EphemeralKeyCreateParams.builder()
                .setCustomer(customerId)
                .setStripeVersion(mobileEphemeralKeyApiVersion)
                .build();

        EphemeralKey ephemeralKey = EphemeralKey.create(params);
        return ephemeralKey.getSecret();
    }

    @lombok.Builder
    @lombok.Data
    public static class StripePaymentSheetInitResult {
        private String paymentIntentId;
        private String paymentIntentClientSecret;
        private String customerId;
        private String customerEphemeralKeySecret;
        private String publishableKey;
    }
}