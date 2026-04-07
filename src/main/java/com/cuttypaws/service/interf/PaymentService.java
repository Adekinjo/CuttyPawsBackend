package com.cuttypaws.service.interf;

import com.cuttypaws.dto.PaymentSheetRequest;
import com.cuttypaws.response.PaymentSheetResponse;

public interface PaymentService {
    PaymentSheetResponse initializePaymentSheet(PaymentSheetRequest request);
    PaymentSheetResponse getPaymentStatus(String reference);
}