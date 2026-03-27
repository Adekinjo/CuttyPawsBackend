package com.cuttypaws.dto;

import com.cuttypaws.enums.ServiceStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceDashboardDto {
    private ServiceStatus status;
    private Boolean canAccessDashboard;
    private String statusMessage;
    private String rejectionReason;
    private ServiceProfileDto serviceProfile;
    private ServiceAdSubscriptionDto activeAdSubscription;
}