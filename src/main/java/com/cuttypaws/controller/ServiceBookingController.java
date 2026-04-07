package com.cuttypaws.controller;

import com.cuttypaws.dto.CreateServiceBookingRequest;
import com.cuttypaws.response.UserResponse;
import com.cuttypaws.service.interf.ServiceBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/service-bookings")
@RequiredArgsConstructor
public class ServiceBookingController {

    private final ServiceBookingService serviceBookingService;

    @PostMapping("/my-bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> createMyBooking(
            @Valid @RequestBody CreateServiceBookingRequest request
    ) {
        UserResponse response = serviceBookingService.createMyBooking(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/my-bookings/{bookingId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getMyBookingStatus(@PathVariable UUID bookingId) {
        UserResponse response = serviceBookingService.getMyBookingStatus(bookingId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getMyBookings() {
        UserResponse response = serviceBookingService.getMyBookings();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/provider/bookings")
    @PreAuthorize("hasAuthority('ROLE_SERVICE_PROVIDER')")
    public ResponseEntity<UserResponse> getMyProviderBookings() {
        UserResponse response = serviceBookingService.getMyProviderBookings();
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}