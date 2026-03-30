package com.cuttypaws.dto;

import com.cuttypaws.enums.BookingReportStatus;
import lombok.Data;

@Data
public class UpdateServiceBookingReportRequest {
    private BookingReportStatus status;
    private String adminNote;
    private Boolean suspendProvider;
}