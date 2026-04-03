package com.cuttypaws.dto;

import com.cuttypaws.enums.BookingReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServiceBookingReportRequest {
    private BookingReportStatus status;
    private String adminNote;
    private Boolean suspendProvider;
}