package com.cuttypaws.service.impl;

import com.cuttypaws.dto.ConfirmServiceBookingPaymentRequest;
import com.cuttypaws.dto.ServiceBookingDto;
import com.cuttypaws.dto.CreateServiceBookingRequest;
import com.cuttypaws.entity.Payment;
import com.cuttypaws.entity.ServiceBooking;
import com.cuttypaws.entity.ServiceProfile;
import com.cuttypaws.entity.User;
import com.cuttypaws.enums.BookingStatus;
import com.cuttypaws.enums.PaymentProvider;
import com.cuttypaws.enums.PaymentPurpose;
import com.cuttypaws.enums.PaymentStatus;
import com.cuttypaws.enums.ServiceStatus;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.mapper.ServiceBookingMapper;
import com.cuttypaws.repository.PaymentRepo;
import com.cuttypaws.repository.ServiceBookingRepo;
import com.cuttypaws.repository.ServiceProfileRepo;
import com.cuttypaws.repository.UserRepo;
import com.cuttypaws.response.UserResponse;
import com.cuttypaws.service.interf.ServiceBookingService;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cuttypaws.service.EmailService;
import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime;
import java.util.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceBookingServiceImpl implements ServiceBookingService {

    private final ServiceBookingRepo serviceBookingRepo;
    private final ServiceProfileRepo serviceProfileRepo;
    private final PaymentRepo paymentRepo;
    private final UserRepo userRepo;
    private final StripeService stripeService;
    private final ServiceBookingMapper serviceBookingMapper;
    private final EmailService emailService;

    @Override
    @Transactional
    public UserResponse createMyBooking(CreateServiceBookingRequest request) {
        User currentUser = getCurrentUser();

        if (request.getPaymentPurpose() != PaymentPurpose.SERVICE_BOOKING) {
            return UserResponse.builder()
                    .status(400)
                    .message("paymentPurpose must be SERVICE_BOOKING")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }

        ServiceProfile profile = serviceProfileRepo.findById(request.getServiceProfileId())
                .orElseThrow(() -> new NotFoundException("Service profile not found"));

        if (profile.getStatus() != ServiceStatus.ACTIVE) {
            return UserResponse.builder()
                    .status(400)
                    .message("Only active service providers can receive bookings")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }

        if (profile.getUser() != null && profile.getUser().getId().equals(currentUser.getId())) {
            return UserResponse.builder()
                    .status(400)
                    .message("You cannot book your own service")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }

        validateBookingRequest(request, profile);

        boolean conflictExists = serviceBookingRepo
                .existsByServiceProfileIdAndBookingStatusInAndStartsAtLessThanAndEndsAtGreaterThan(
                        profile.getId(),
                        Set.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED),
                        request.getEndsAt(),
                        request.getStartsAt()
                );

        if (conflictExists) {
            return UserResponse.builder()
                    .status(409)
                    .message("This provider already has a booking in the selected time range")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }

        String paymentReference = buildBookingReference();

        ServiceBooking booking = ServiceBooking.builder()
                .customer(currentUser)
                .serviceProfile(profile)
                .serviceType(profile.getServiceType())
                .petName(trimToNull(request.getPetName()))
                .petType(trimToNull(request.getPetType()))
                .serviceAddress(trimToNull(request.getServiceAddress()))
                .notes(trimToNull(request.getNotes()))
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .timezone(request.getTimezone() == null || request.getTimezone().isBlank()
                        ? "America/Chicago"
                        : request.getTimezone().trim())
                .amount(request.getAmount())
                .bookingStatus(BookingStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentReference(paymentReference)
                .build();

        ServiceBooking savedBooking = serviceBookingRepo.save(booking);

        Payment payment = new Payment();
        payment.setAmount(savedBooking.getAmount());
        payment.setEmail(currentUser.getEmail());
        payment.setCurrency("USD");
        payment.setProvider(PaymentProvider.STRIPE);
        payment.setMethod("STRIPE");
        payment.setReference(paymentReference);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentPurpose(PaymentPurpose.SERVICE_BOOKING);
        payment.setUser(currentUser);
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setServiceBooking(savedBooking);

        Payment savedPayment = paymentRepo.save(payment);

        try {
            Session session = stripeService.createServiceBookingCheckoutSession(savedBooking, currentUser.getEmail());

            savedPayment.setCheckoutSessionId(session.getId());
            savedPayment.setPaymentUrl(session.getUrl());
            paymentRepo.save(savedPayment);

            ServiceBookingDto dto = serviceBookingMapper.toDto(savedBooking, savedPayment);

            return UserResponse.builder()
                    .status(201)
                    .message("Booking created successfully. Complete payment to confirm the booking.")
                    .serviceBooking(dto)
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to create Stripe checkout for service booking", e);
            return UserResponse.builder()
                    .status(500)
                    .message("Unable to create booking checkout: " + e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional
    public UserResponse confirmMyBookingPayment(ConfirmServiceBookingPaymentRequest request) {
        ServiceBooking booking = serviceBookingRepo.findByPaymentReference(request.getPaymentReference())
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        Payment payment = paymentRepo.findByReference(request.getPaymentReference())
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        try {
            Session session = stripeService.retrieveCheckoutSession(payment.getCheckoutSessionId());

            if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepo.save(payment);

                booking.setPaymentStatus(PaymentStatus.FAILED);
                booking.setBookingStatus(BookingStatus.EXPIRED);
                serviceBookingRepo.save(booking);

                return UserResponse.builder()
                        .status(400)
                        .message("Payment verification failed")
                        .serviceBooking(serviceBookingMapper.toDto(booking, payment))
                        .timeStamp(LocalDateTime.now())
                        .build();
            }

            LocalDateTime now = LocalDateTime.now();

            payment.setStatus(PaymentStatus.PAID);
            payment.setPaymentIntentId(session.getPaymentIntent());
            paymentRepo.save(payment);

            booking.setPaymentStatus(PaymentStatus.PAID);
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            booking.setConfirmedAt(now);
            serviceBookingRepo.save(booking);
            sendBookingConfirmationEmails(booking);

            return UserResponse.builder()
                    .status(200)
                    .message("Booking payment confirmed successfully")
                    .serviceBooking(serviceBookingMapper.toDto(booking, payment))
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error confirming booking payment", e);
            return UserResponse.builder()
                    .status(500)
                    .message("Unable to confirm booking payment: " + e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyBookings() {
        User currentUser = getCurrentUser();

        List<ServiceBookingDto> bookings = serviceBookingRepo
                .findByCustomerIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(serviceBookingMapper::toDto)
                .toList();

        return UserResponse.builder()
                .status(200)
                .message("Bookings retrieved successfully")
                .serviceBookings(bookings)
                .timeStamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProviderBookings() {
        User currentUser = getCurrentUser();

        List<ServiceBookingDto> bookings = serviceBookingRepo
                .findByServiceProfileUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(serviceBookingMapper::toDto)
                .toList();

        return UserResponse.builder()
                .status(200)
                .message("Provider bookings retrieved successfully")
                .serviceBookings(bookings)
                .timeStamp(LocalDateTime.now())
                .build();
    }

    private void validateBookingRequest(CreateServiceBookingRequest request, ServiceProfile profile) {
        if (request.getStartsAt() == null || request.getEndsAt() == null) {
            throw new RuntimeException("Booking start and end times are required");
        }

        if (!request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new RuntimeException("endsAt must be after startsAt");
        }

        if (request.getStartsAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Booking time must be in the future");
        }

        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new RuntimeException("Booking amount must be greater than zero");
        }

        if (profile.getPriceFrom() != null && request.getAmount().compareTo(profile.getPriceFrom()) < 0) {
            throw new RuntimeException("Booking amount cannot be below the provider minimum price");
        }

        if (profile.getPriceTo() != null && request.getAmount().compareTo(profile.getPriceTo()) > 0) {
            throw new RuntimeException("Booking amount cannot be above the provider maximum price");
        }
    }

    private String buildBookingReference() {
        return "SBK-" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase();
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

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private void sendBookingConfirmationEmails(ServiceBooking booking) {
        try {
            User customer = booking.getCustomer();
            ServiceProfile profile = booking.getServiceProfile();
            User provider = profile != null ? profile.getUser() : null;

            if (provider != null && provider.getEmail() != null && !provider.getEmail().isBlank()) {
                emailService.sendTemplateEmail(
                        provider.getEmail(),
                        "New Service Booking on CuttyPaws",
                        "email/booking-provider",
                        buildProviderBookingTemplateVariables(booking)
                );
            }

            if (customer != null && customer.getEmail() != null && !customer.getEmail().isBlank()) {
                emailService.sendTemplateEmail(
                        customer.getEmail(),
                        "Your CuttyPaws Service Booking is Confirmed",
                        "email/booking-customer",
                        buildCustomerBookingTemplateVariables(booking)
                );
            }
        } catch (Exception e) {
            log.error("Failed to queue booking confirmation emails for booking {}", booking.getId(), e);
        }
    }

    private Map<String, Object> buildProviderBookingTemplateVariables(ServiceBooking booking) {
        Map<String, Object> vars = new HashMap<>();

        User customer = booking.getCustomer();
        ServiceProfile profile = booking.getServiceProfile();
        User provider = profile != null ? profile.getUser() : null;

        vars.put("providerName", safe(provider != null ? provider.getName() : null));
        vars.put("businessName", safe(profile != null ? profile.getBusinessName() : null));
        vars.put("serviceType", booking.getServiceType() != null ? booking.getServiceType().name() : "N/A");
        vars.put("customerName", safe(customer != null ? customer.getName() : null));
        vars.put("customerEmail", safe(customer != null ? customer.getEmail() : null));
        vars.put("customerPhone", safe(customer != null ? customer.getPhoneNumber() : null));
        vars.put("petName", safe(booking.getPetName()));
        vars.put("petType", safe(booking.getPetType()));
        vars.put("serviceAddress", safe(booking.getServiceAddress()));
        vars.put("notes", safe(booking.getNotes()));
        vars.put("startTime", formatDateTime(booking.getStartsAt()));
        vars.put("endTime", formatDateTime(booking.getEndsAt()));
        vars.put("timezone", safe(booking.getTimezone()));
        vars.put("amountPaid", safeAmount(booking.getAmount()));
        vars.put("paymentReference", safe(booking.getPaymentReference()));
        vars.put("bookingStatus", booking.getBookingStatus() != null ? booking.getBookingStatus().name() : "N/A");
        vars.put("paymentStatus", booking.getPaymentStatus() != null ? booking.getPaymentStatus().name() : "N/A");

        return vars;
    }

    private Map<String, Object> buildCustomerBookingTemplateVariables(ServiceBooking booking) {
        Map<String, Object> vars = new HashMap<>();

        ServiceProfile profile = booking.getServiceProfile();
        User provider = profile != null ? profile.getUser() : null;
        User customer = booking.getCustomer();

        vars.put("customerName", safe(customer != null ? customer.getName() : null));
        vars.put("providerName", safe(provider != null ? provider.getName() : null));
        vars.put("businessName", safe(profile != null ? profile.getBusinessName() : null));
        vars.put("serviceType", booking.getServiceType() != null ? booking.getServiceType().name() : "N/A");
        vars.put("petName", safe(booking.getPetName()));
        vars.put("petType", safe(booking.getPetType()));
        vars.put("serviceAddress", safe(booking.getServiceAddress()));
        vars.put("notes", safe(booking.getNotes()));
        vars.put("startTime", formatDateTime(booking.getStartsAt()));
        vars.put("endTime", formatDateTime(booking.getEndsAt()));
        vars.put("timezone", safe(booking.getTimezone()));
        vars.put("amountPaid", safeAmount(booking.getAmount()));
        vars.put("paymentReference", safe(booking.getPaymentReference()));
        vars.put("bookingStatus", booking.getBookingStatus() != null ? booking.getBookingStatus().name() : "N/A");
        vars.put("paymentStatus", booking.getPaymentStatus() != null ? booking.getPaymentStatus().name() : "N/A");

        return vars;
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "N/A";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a");
        return value.format(formatter);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private String safeAmount(java.math.BigDecimal amount) {
        return amount == null ? "0.00" : amount.toPlainString();
    }
}