package com.cuttypaws.service.interf;

import com.cuttypaws.dto.ConfirmServiceBookingPaymentRequest;
import com.cuttypaws.dto.CreateServiceBookingRequest;
import com.cuttypaws.response.UserResponse;

public interface ServiceBookingService {
    UserResponse createMyBooking(CreateServiceBookingRequest request);
    UserResponse confirmMyBookingPayment(ConfirmServiceBookingPaymentRequest request);
    UserResponse getMyBookings();
    UserResponse getMyProviderBookings();
}