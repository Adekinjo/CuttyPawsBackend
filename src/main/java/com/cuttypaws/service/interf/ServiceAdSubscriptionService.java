package com.cuttypaws.service.interf;

import com.cuttypaws.dto.ConfirmServiceAdPaymentRequest;
import com.cuttypaws.dto.CreateServiceAdSubscriptionRequest;
import com.cuttypaws.response.UserResponse;

public interface ServiceAdSubscriptionService {
    UserResponse createMyAdSubscription(CreateServiceAdSubscriptionRequest request);
    UserResponse confirmMyAdPayment(ConfirmServiceAdPaymentRequest request);
    UserResponse getMyAdSubscriptions();
    UserResponse getMyActiveAdSubscription();
    void expireOldSubscriptions();
}