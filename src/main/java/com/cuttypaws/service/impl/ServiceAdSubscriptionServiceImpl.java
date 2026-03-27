package com.cuttypaws.service.impl;

import com.cuttypaws.dto.CreateServiceAdSubscriptionRequest;
import com.cuttypaws.dto.ServiceAdSubscriptionDto;
import com.cuttypaws.entity.Payment;
import com.cuttypaws.entity.ServiceAdSubscription;
import com.cuttypaws.entity.ServiceProfile;
import com.cuttypaws.entity.User;
import com.cuttypaws.enums.AdPlanType;
import com.cuttypaws.enums.PaymentProvider;
import com.cuttypaws.enums.PaymentStatus;
import com.cuttypaws.enums.ServiceStatus;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.mapper.ServiceAdSubscriptionMapper;
import com.cuttypaws.repository.PaymentRepo;
import com.cuttypaws.repository.ServiceAdSubscriptionRepo;
import com.cuttypaws.repository.ServiceProfileRepo;
import com.cuttypaws.repository.UserRepo;
import com.cuttypaws.response.UserResponse;
import com.cuttypaws.service.interf.ServiceAdSubscriptionService;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceAdSubscriptionServiceImpl implements ServiceAdSubscriptionService {

    private final ServiceAdSubscriptionRepo serviceAdSubscriptionRepo;
    private final ServiceProfileRepo serviceProfileRepo;
    private final UserRepo userRepo;
    private final PaymentRepo paymentRepo;
    private final StripeService stripeService;
    private final ServiceAdSubscriptionMapper mapper;

    @Override
    @Transactional
    public UserResponse createMyAdSubscription(CreateServiceAdSubscriptionRequest request) {
        User currentUser = getCurrentUser();

        ServiceProfile profile = serviceProfileRepo.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Service profile not found"));

        if (profile.getStatus() != ServiceStatus.ACTIVE) {
            return UserResponse.builder()
                    .status(403)
                    .message("Only approved service providers can subscribe for adverts")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }

        BigDecimal amount = resolvePlanPrice(request.getPlanType());
        String paymentReference = generatePaymentReference();

        ServiceAdSubscription subscription = ServiceAdSubscription.builder()
                .serviceProfile(profile)
                .planType(request.getPlanType())
                .amount(amount)
                .currency("USD")
                .paymentProvider(PaymentProvider.STRIPE)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentReference(paymentReference)
                .isActive(false)
                .build();

        ServiceAdSubscription savedSubscription = serviceAdSubscriptionRepo.save(subscription);

        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setEmail(currentUser.getEmail());
        payment.setCurrency("USD");
        payment.setProvider(PaymentProvider.STRIPE);
        payment.setMethod("STRIPE"); // fix
        payment.setReference(paymentReference);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentPurpose(com.cuttypaws.enums.PaymentPurpose.SERVICE_AD);
        payment.setUser(currentUser);
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setServiceAdSubscription(savedSubscription);

        Payment savedPayment = paymentRepo.save(payment);

        try {
            Session session = stripeService.createServiceAdCheckoutSession(savedSubscription, currentUser.getEmail());

            savedSubscription.setCheckoutSessionId(session.getId());
            savedSubscription.setPaymentUrl(session.getUrl());
            serviceAdSubscriptionRepo.save(savedSubscription);

            savedPayment.setCheckoutSessionId(session.getId());
            savedPayment.setPaymentUrl(session.getUrl());
            paymentRepo.save(savedPayment);

            return UserResponse.builder()
                    .status(201)
                    .message("Advert subscription created successfully")
                    .serviceAdSubscription(mapper.toDto(savedSubscription))
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to create Stripe checkout for service ad", e);
            return UserResponse.builder()
                    .status(500)
                    .message("Unable to create advert checkout: " + e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional
    public UserResponse confirmMyAdPayment(com.cuttypaws.dto.ConfirmServiceAdPaymentRequest request) {
        ServiceAdSubscription subscription = serviceAdSubscriptionRepo.findByPaymentReference(request.getPaymentReference())
                .orElseThrow(() -> new NotFoundException("Ad subscription not found"));

        Payment payment = paymentRepo.findByReference(request.getPaymentReference())
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        try {
            Session session = stripeService.retrieveCheckoutSession(subscription.getCheckoutSessionId());

            if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepo.save(payment);

                subscription.setPaymentStatus(PaymentStatus.FAILED);
                subscription.setIsActive(false);
                serviceAdSubscriptionRepo.save(subscription);

                return UserResponse.builder()
                        .status(400)
                        .message("Payment verification failed")
                        .serviceAdSubscription(mapper.toDto(subscription))
                        .timeStamp(LocalDateTime.now())
                        .build();
            }

            deactivateExistingActiveSubscriptions(subscription.getServiceProfile().getId());

            LocalDateTime now = LocalDateTime.now();

            payment.setStatus(PaymentStatus.PAID);
            payment.setPaymentIntentId(session.getPaymentIntent());
            paymentRepo.save(payment);

            subscription.setPaymentStatus(PaymentStatus.PAID);
            subscription.setPaymentIntentId(session.getPaymentIntent());
            subscription.setIsActive(true);
            subscription.setPaidAt(now);
            subscription.setStartsAt(now);
            subscription.setEndsAt(now.plusDays(resolvePlanDurationDays(subscription.getPlanType())));
            serviceAdSubscriptionRepo.save(subscription);

            return UserResponse.builder()
                    .status(200)
                    .message("Advert payment confirmed and promotion activated successfully")
                    .serviceAdSubscription(mapper.toDto(subscription))
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error confirming advert payment", e);
            return UserResponse.builder()
                    .status(500)
                    .message("Unable to confirm advert payment: " + e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyAdSubscriptions() {
        User currentUser = getCurrentUser();

        List<ServiceAdSubscriptionDto> adSubscriptions = serviceAdSubscriptionRepo
                .findByServiceProfileUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(mapper::toDto)
                .toList();

        return UserResponse.builder()
                .status(200)
                .message("Advert subscriptions retrieved successfully")
                .serviceAdSubscriptions(adSubscriptions)
                .timeStamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyActiveAdSubscription() {
        User currentUser = getCurrentUser();

        ServiceProfile profile = serviceProfileRepo.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Service profile not found"));

        ServiceAdSubscription activeSubscription = serviceAdSubscriptionRepo
                .findFirstByServiceProfileIdAndIsActiveTrueAndEndsAtAfterOrderByCreatedAtDesc(profile.getId(), LocalDateTime.now())
                .orElse(null);

        return UserResponse.builder()
                .status(200)
                .message("Active advert subscription retrieved successfully")
                .serviceAdSubscription(mapper.toDto(activeSubscription))
                .timeStamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void expireOldSubscriptions() {
        List<ServiceAdSubscription> expiredSubscriptions =
                serviceAdSubscriptionRepo.findByIsActiveTrueAndEndsAtBefore(LocalDateTime.now());

        for (ServiceAdSubscription subscription : expiredSubscriptions) {
            subscription.setIsActive(false);
            subscription.setPaymentStatus(PaymentStatus.EXPIRED);
        }

        if (!expiredSubscriptions.isEmpty()) {
            serviceAdSubscriptionRepo.saveAll(expiredSubscriptions);
        }
    }

    private void deactivateExistingActiveSubscriptions(UUID serviceProfileId) {
        ServiceAdSubscription active = serviceAdSubscriptionRepo
                .findFirstByServiceProfileIdAndIsActiveTrueAndEndsAtAfterOrderByCreatedAtDesc(serviceProfileId, LocalDateTime.now())
                .orElse(null);

        if (active != null) {
            active.setIsActive(false);
            serviceAdSubscriptionRepo.save(active);
        }
    }

    private BigDecimal resolvePlanPrice(AdPlanType planType) {
        return switch (planType) {
            case BASIC -> BigDecimal.valueOf(19.99);
            case BOOSTED -> BigDecimal.valueOf(39.99);
            case PREMIUM -> BigDecimal.valueOf(69.99);
            case FEATURED -> BigDecimal.valueOf(99.99);
        };
    }

    private long resolvePlanDurationDays(AdPlanType planType) {
        return switch (planType) {
            case BASIC -> 7;
            case BOOSTED -> 14;
            case PREMIUM -> 21;
            case FEATURED -> 30;
        };
    }

    private String generatePaymentReference() {
        return "AD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();

        return userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Current user not found"));
    }
}