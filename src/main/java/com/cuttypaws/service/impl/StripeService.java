package com.cuttypaws.service.impl;

import com.cuttypaws.entity.Payment;
import com.cuttypaws.entity.ServiceAdSubscription;
import com.cuttypaws.entity.ServiceBooking;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public Session createOrderCheckoutSession(Payment payment) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(payment.getEmail())
                .setSuccessUrl(frontendUrl + "/payment-success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/payment-failed")
                .putMetadata("paymentId", String.valueOf(payment.getId()))
                .putMetadata("reference", payment.getReference())
                .putMetadata("paymentType", "ORDER")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(payment.getCurrency().toLowerCase())
                                                .setUnitAmount(payment.getAmount().movePointRight(2).longValueExact())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("CuttyPaws Order Payment")
                                                                .setDescription("Order payment for CuttyPaws marketplace")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        return Session.create(params);
    }

    public Session createServiceAdCheckoutSession(ServiceAdSubscription subscription, String customerEmail) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(customerEmail)
                .setSuccessUrl(frontendUrl + "/service-ads/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/service-ads/cancel")
                .putMetadata("subscriptionId", subscription.getId().toString())
                .putMetadata("reference", subscription.getPaymentReference())
                .putMetadata("paymentType", "SERVICE_AD")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(subscription.getCurrency().toLowerCase())
                                                .setUnitAmount(subscription.getAmount().movePointRight(2).longValueExact())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("CuttyPaws " + subscription.getPlanType().name() + " Promotion")
                                                                .setDescription("Service promotion payment")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        return Session.create(params);
    }

    public Session createServiceBookingCheckoutSession(ServiceBooking booking, String customerEmail) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(customerEmail)
                .setSuccessUrl(frontendUrl + "/service-bookings/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/service-bookings/cancel")
                .putMetadata("bookingId", booking.getId().toString())
                .putMetadata("reference", booking.getPaymentReference())
                .putMetadata("paymentType", "SERVICE_BOOKING")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(booking.getAmount().movePointRight(2).longValueExact())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("CuttyPaws Service Booking")
                                                                .setDescription(
                                                                        "Booking for " +
                                                                                (booking.getServiceProfile().getBusinessName() != null
                                                                                        ? booking.getServiceProfile().getBusinessName()
                                                                                        : booking.getServiceProfile().getServiceType().name())
                                                                )
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        return Session.create(params);
    }

    public Session retrieveCheckoutSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }
}