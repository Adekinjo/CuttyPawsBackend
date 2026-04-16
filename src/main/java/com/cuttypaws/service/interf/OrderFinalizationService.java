package com.cuttypaws.service.interf;

import com.cuttypaws.entity.Payment;

public interface OrderFinalizationService {
    void finalizeOrderFromPayment(Payment payment);
}