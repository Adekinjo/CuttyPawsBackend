package com.cuttypaws.controller;

import com.cuttypaws.dto.CreateServiceBookingReportRequest;
import com.cuttypaws.dto.UpdateServiceBookingReportRequest;
import com.cuttypaws.response.UserResponse;
import com.cuttypaws.service.interf.ServiceBookingReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/service-booking-reports")
@RequiredArgsConstructor
public class ServiceBookingReportController {

    private final ServiceBookingReportService serviceBookingReportService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> createMyBookingReport(
            @Valid @RequestBody CreateServiceBookingReportRequest request
    ) {
        UserResponse response = serviceBookingReportService.createMyBookingReport(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/my-reports")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getMyBookingReports() {
        UserResponse response = serviceBookingReportService.getMyBookingReports();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> getAllBookingReports() {
        UserResponse response = serviceBookingReportService.getAllBookingReports();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/admin/{reportId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> getBookingReportById(@PathVariable UUID reportId) {
        UserResponse response = serviceBookingReportService.getBookingReportById(reportId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/admin/{reportId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> updateBookingReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody UpdateServiceBookingReportRequest request
    ) {
        UserResponse response = serviceBookingReportService.updateBookingReport(reportId, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}