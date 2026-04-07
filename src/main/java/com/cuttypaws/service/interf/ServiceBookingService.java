package com.cuttypaws.service.interf;

import com.cuttypaws.dto.ConfirmServiceBookingPaymentRequest;
import com.cuttypaws.dto.CreateServiceBookingRequest;
import com.cuttypaws.entity.ServiceBooking;
import com.cuttypaws.response.UserResponse;

import java.util.UUID;

public interface ServiceBookingService {
    UserResponse createMyBooking(CreateServiceBookingRequest request);
    UserResponse getMyBookings();
    UserResponse getMyProviderBookings();
    UserResponse getMyBookingStatus(UUID bookingId);

    void sendBookingConfirmationEmails(ServiceBooking booking);
}