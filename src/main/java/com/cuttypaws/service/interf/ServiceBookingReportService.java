package com.cuttypaws.service.interf;

import com.cuttypaws.dto.CreateServiceBookingReportRequest;
import com.cuttypaws.dto.UpdateServiceBookingReportRequest;
import com.cuttypaws.response.UserResponse;

import java.util.UUID;

public interface ServiceBookingReportService {
    UserResponse createMyBookingReport(CreateServiceBookingReportRequest request);
    UserResponse getMyBookingReports();
    UserResponse getAllBookingReports();
    UserResponse getBookingReportById(UUID reportId);
    UserResponse updateBookingReport(UUID reportId, UpdateServiceBookingReportRequest request);
}