package com.cuttypaws.service.interf;

import com.cuttypaws.dto.CreateCheckoutSessionRequest;
import com.cuttypaws.response.CheckoutSessionResponse;

public interface CheckoutService {
    CheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request);
}