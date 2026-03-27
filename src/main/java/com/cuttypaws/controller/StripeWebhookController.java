package com.cuttypaws.controller;

import com.cuttypaws.entity.Payment;
import com.cuttypaws.entity.ServiceAdSubscription;
import com.cuttypaws.enums.PaymentStatus;
import com.cuttypaws.repository.PaymentRepo;
import com.cuttypaws.repository.ServiceAdSubscriptionRepo;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/webhook/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final PaymentRepo paymentRepo;
    private final ServiceAdSubscriptionRepo serviceAdSubscriptionRepo;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() -> new RuntimeException("Unable to deserialize checkout session"));

                String sessionId = session.getId();

                paymentRepo.findByCheckoutSessionId(sessionId).ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setPaymentIntentId(session.getPaymentIntent());
                    paymentRepo.save(payment);
                });

                serviceAdSubscriptionRepo.findByCheckoutSessionId(sessionId).ifPresent(subscription -> {
                    if (!Boolean.TRUE.equals(subscription.getIsActive())) {
                        subscription.setPaymentStatus(PaymentStatus.PAID);
                        subscription.setPaymentIntentId(session.getPaymentIntent());
                        subscription.setPaidAt(LocalDateTime.now());
                        subscription.setIsActive(true);
                        subscription.setStartsAt(LocalDateTime.now());

                        long days = switch (subscription.getPlanType()) {
                            case BASIC -> 7;
                            case BOOSTED -> 14;
                            case PREMIUM -> 21;
                            case FEATURED -> 30;
                        };

                        subscription.setEndsAt(LocalDateTime.now().plusDays(days));
                        serviceAdSubscriptionRepo.save(subscription);
                    }
                });
            }

            return ResponseEntity.ok("Webhook handled");

        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature", e);
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            log.error("Stripe webhook error", e);
            return ResponseEntity.internalServerError().body("Webhook error");
        }
    }
}