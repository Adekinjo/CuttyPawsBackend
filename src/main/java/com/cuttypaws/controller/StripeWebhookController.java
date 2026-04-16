package com.cuttypaws.controller;

import com.cuttypaws.entity.Payment;
import com.cuttypaws.entity.ServiceAdSubscription;
import com.cuttypaws.entity.ServiceBooking;
import com.cuttypaws.enums.BookingStatus;
import com.cuttypaws.enums.PaymentStatus;
import com.cuttypaws.repository.PaymentRepo;
import com.cuttypaws.repository.ServiceAdSubscriptionRepo;
import com.cuttypaws.repository.ServiceBookingRepo;
import com.cuttypaws.service.interf.OrderFinalizationService;
import com.cuttypaws.service.interf.ServiceBookingService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
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
    private final ServiceBookingRepo serviceBookingRepo;
    private final ServiceBookingService serviceBookingService;
    private final OrderFinalizationService orderFinalizationService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        try {
            log.info("Stripe webhook received. Signature header present={}", sigHeader != null);

            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("Stripe webhook event type={}", event.getType());

            switch (event.getType()) {
                case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
                case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
                case "payment_intent.canceled" -> handlePaymentIntentCanceled(event);
                default -> log.info("Unhandled Stripe event type={}", event.getType());
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

    private void handlePaymentIntentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Unable to deserialize payment intent"));

        String paymentIntentId = paymentIntent.getId();
        log.info("Webhook received payment_intent.succeeded for paymentIntentId={}", paymentIntentId);

        Payment payment = paymentRepo.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> {
                    log.error("Payment not found for paymentIntentId={}", paymentIntentId);
                    return new RuntimeException("Payment not found for payment intent: " + paymentIntentId);
                });

        log.info("Matched payment id={} reference={} currentStatus={}",
                payment.getId(), payment.getReference(), payment.getStatus());

        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Payment already marked as PAID for paymentIntentId={}", paymentIntentId);
            return;
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaymentIntentId(paymentIntentId);
        paymentRepo.save(payment);

        log.info("Payment marked as PAID for paymentId={} reference={}",
                payment.getId(), payment.getReference());

        switch (payment.getPaymentPurpose()) {
            case ORDER -> fulfillOrderPayment(payment);
            case SERVICE_AD -> fulfillServiceAdPayment(payment);
            case SERVICE_BOOKING -> fulfillServiceBookingPayment(payment);
        }
    }

    private void handlePaymentIntentFailed(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Unable to deserialize payment intent"));

        paymentRepo.findByPaymentIntentId(paymentIntent.getId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepo.save(payment);

            if (payment.getServiceAdSubscription() != null) {
                ServiceAdSubscription subscription = payment.getServiceAdSubscription();
                subscription.setPaymentStatus(PaymentStatus.FAILED);
                subscription.setIsActive(false);
                serviceAdSubscriptionRepo.save(subscription);
            }

            if (payment.getServiceBooking() != null) {
                ServiceBooking booking = payment.getServiceBooking();
                booking.setPaymentStatus(PaymentStatus.FAILED);
                booking.setBookingStatus(BookingStatus.EXPIRED);
                serviceBookingRepo.save(booking);
            }
        });
    }

    private void handlePaymentIntentCanceled(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Unable to deserialize payment intent"));

        paymentRepo.findByPaymentIntentId(paymentIntent.getId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepo.save(payment);
        });
    }

    private void fulfillOrderPayment(Payment payment) {
        if (payment.getOrder() != null) {
            log.info("Order already exists for payment {}", payment.getId());
            return;
        }

        orderFinalizationService.finalizeOrderFromPayment(payment);
        log.info("Order created successfully for payment {}", payment.getId());
    }

    private void fulfillServiceAdPayment(Payment payment) {
        ServiceAdSubscription subscription = payment.getServiceAdSubscription();
        if (subscription == null) return;

        if (Boolean.TRUE.equals(subscription.getIsActive())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        subscription.setPaymentStatus(PaymentStatus.PAID);
        subscription.setPaymentIntentId(payment.getPaymentIntentId());
        subscription.setPaidAt(now);
        subscription.setIsActive(true);
        subscription.setStartsAt(now);

        long days = switch (subscription.getPlanType()) {
            case BASIC -> 7;
            case BOOSTED -> 14;
            case PREMIUM -> 21;
            case FEATURED -> 30;
        };

        subscription.setEndsAt(now.plusDays(days));
        serviceAdSubscriptionRepo.save(subscription);
    }

    private void fulfillServiceBookingPayment(Payment payment) {
        ServiceBooking booking = payment.getServiceBooking();
        if (booking == null) return;

        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            return;
        }

        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        serviceBookingRepo.save(booking);

        ServiceBooking savedBooking = serviceBookingRepo.save(booking);
        serviceBookingService.sendBookingConfirmationEmails(savedBooking);
    }
}