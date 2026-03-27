package com.cuttypaws.service.interf;

import com.cuttypaws.dto.*;
import com.cuttypaws.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse initializePayment(PaymentRequest request);
    PaymentResponse verifyPayment(String reference);

    PaymentResponse getPaymentByReference(String reference);
}

