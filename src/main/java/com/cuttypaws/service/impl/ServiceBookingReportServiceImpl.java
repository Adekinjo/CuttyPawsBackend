package com.cuttypaws.service.impl;

import com.cuttypaws.dto.CreateServiceBookingReportRequest;
import com.cuttypaws.dto.UpdateServiceBookingReportRequest;
import com.cuttypaws.entity.ServiceBooking;
import com.cuttypaws.entity.ServiceBookingReport;
import com.cuttypaws.entity.ServiceProfile;
import com.cuttypaws.entity.User;
import com.cuttypaws.enums.BookingReportStatus;
import com.cuttypaws.enums.BookingStatus;
import com.cuttypaws.enums.ServiceStatus;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.mapper.ServiceBookingReportMapper;
import com.cuttypaws.repository.ServiceBookingRepo;
import com.cuttypaws.repository.ServiceBookingReportRepo;
import com.cuttypaws.repository.ServiceProfileRepo;
import com.cuttypaws.repository.UserRepo;
import com.cuttypaws.response.UserResponse;
import com.cuttypaws.service.EmailService;
import com.cuttypaws.service.interf.ServiceBookingReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceBookingReportServiceImpl implements ServiceBookingReportService {

    private final ServiceBookingRepo serviceBookingRepo;
    private final ServiceBookingReportRepo serviceBookingReportRepo;
    private final ServiceProfileRepo serviceProfileRepo;
    private final UserRepo userRepo;
    private final ServiceBookingReportMapper serviceBookingReportMapper;
    private final EmailService emailService;

    @Value("${app.service-review.admin-email:cuttypawsinfo@gmail.com}")
    private String adminReviewEmail;

    @Override
    @Transactional
    public UserResponse createMyBookingReport(CreateServiceBookingReportRequest request) {
        User currentUser = getCurrentUser();

        ServiceBooking booking = serviceBookingRepo.findById(request.getBookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (!booking.getCustomer().getId().equals(currentUser.getId())) {
            return UserResponse.builder()
                    .status(403)
                    .message("You can only report your own booking")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }

        if (serviceBookingReportRepo.existsByBookingIdAndCustomerId(booking.getId(), currentUser.getId())) {
            return UserResponse.builder()
                    .status(409)
                    .message("You have already reported this booking")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED &&
                booking.getBookingStatus() != BookingStatus.COMPLETED) {
            return UserResponse.builder()
                    .status(400)
                    .message("Only confirmed or completed bookings can be reported")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }

        ServiceBookingReport report = ServiceBookingReport.builder()
                .booking(booking)
                .customer(currentUser)
                .providerUser(booking.getServiceProfile().getUser())
                .reason(request.getReason())
                .details(request.getDetails().trim())
                .status(BookingReportStatus.OPEN)
                .build();

        ServiceBookingReport saved = serviceBookingReportRepo.save(report);

        try {
            // email admin
            emailService.sendEmail(
                    adminReviewEmail,
                    "New Service Booking Report Submitted",
                    "A new service booking report has been submitted.\n\n" +
                            "Booking Ref: " + booking.getPaymentReference() + "\n" +
                            "Customer: " + currentUser.getName() + "\n" +
                            "Customer Email: " + currentUser.getEmail() + "\n" +
                            "Provider: " + booking.getServiceProfile().getUser().getName() + "\n" +
                            "Business Name: " + booking.getServiceProfile().getBusinessName() + "\n" +
                            "Service Type: " + booking.getServiceType() + "\n" +
                            "Amount: " + booking.getAmount() + "\n" +
                            "Booking Start: " + booking.getStartsAt() + "\n" +
                            "Booking End: " + booking.getEndsAt() + "\n" +
                            "Reason: " + request.getReason() + "\n" +
                            "Details: " + request.getDetails()
            );

            // email reporting user
            if (currentUser.getEmail() != null && !currentUser.getEmail().isBlank()) {
                emailService.sendEmail(
                        currentUser.getEmail(),
                        "Your CuttyPaws Service Report Has Been Received",
                        "Dear " + currentUser.getName() + ",\n\n" +
                                "We have received your report regarding a booked service.\n\n" +
                                "Booking Reference: " + booking.getPaymentReference() + "\n" +
                                "Provider: " + booking.getServiceProfile().getUser().getName() + "\n" +
                                "Business Name: " + booking.getServiceProfile().getBusinessName() + "\n" +
                                "Reason: " + request.getReason() + "\n\n" +
                                "Our admin team will review your report and take the necessary action.\n\n" +
                                "Thank you,\n" +
                                "CuttyPaws Team"
                );
            }
        } catch (Exception e) {
            log.warn("Failed to send booking report email notification: {}", e.getMessage());
        }

        return UserResponse.builder()
                .status(201)
                .message("Booking report submitted successfully")
                .serviceBookingReport(serviceBookingReportMapper.toDto(saved))
                .timeStamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyBookingReports() {
        User currentUser = getCurrentUser();

        return UserResponse.builder()
                .status(200)
                .message("Booking reports retrieved successfully")
                .serviceBookingReports(
                        serviceBookingReportRepo.findByCustomerIdOrderByCreatedAtDesc(currentUser.getId())
                                .stream()
                                .map(serviceBookingReportMapper::toDto)
                                .toList()
                )
                .timeStamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getAllBookingReports() {
        return UserResponse.builder()
                .status(200)
                .message("All booking reports retrieved successfully")
                .serviceBookingReports(
                        serviceBookingReportRepo.findAllByOrderByCreatedAtDesc()
                                .stream()
                                .map(serviceBookingReportMapper::toDto)
                                .toList()
                )
                .timeStamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getBookingReportById(UUID reportId) {
        ServiceBookingReport report = serviceBookingReportRepo.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Booking report not found"));

        return UserResponse.builder()
                .status(200)
                .message("Booking report retrieved successfully")
                .serviceBookingReport(serviceBookingReportMapper.toDto(report))
                .timeStamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public UserResponse updateBookingReport(UUID reportId, UpdateServiceBookingReportRequest request) {
        ServiceBookingReport report = serviceBookingReportRepo.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Booking report not found"));

        if (request.getStatus() != null) {
            report.setStatus(request.getStatus());
        }

        if (request.getAdminNote() != null) {
            report.setAdminNote(request.getAdminNote().trim());
        }

        report.setReviewedAt(LocalDateTime.now());

        if (report.getStatus() == BookingReportStatus.RESOLVED) {
            report.setResolvedAt(LocalDateTime.now());
        }

        if (Boolean.TRUE.equals(request.getSuspendProvider())) {
            User providerUser = report.getProviderUser();

            ServiceProfile profile = serviceProfileRepo.findByUserId(providerUser.getId())
                    .orElseThrow(() -> new NotFoundException("Service profile not found"));

            profile.setStatus(ServiceStatus.SUSPENDED);
            serviceProfileRepo.save(profile);

            try {
                emailService.sendEmail(
                        providerUser.getEmail(),
                        "Your CuttyPaws Service Account Has Been Suspended",
                        "Your service account has been suspended while we review a customer complaint."
                );
            } catch (Exception e) {
                log.warn("Failed to email provider suspension notice: {}", e.getMessage());
            }
        }

        ServiceBookingReport updated = serviceBookingReportRepo.save(report);

        return UserResponse.builder()
                .status(200)
                .message("Booking report updated successfully")
                .serviceBookingReport(serviceBookingReportMapper.toDto(updated))
                .timeStamp(LocalDateTime.now())
                .build();
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