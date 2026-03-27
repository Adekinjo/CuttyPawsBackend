package com.cuttypaws.ai.service.interf;

import com.cuttypaws.ai.dto.AiOrderSummaryDto;
import com.cuttypaws.ai.dto.AiPaymentSummaryDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiOrderSupportService {
    List<AiOrderSummaryDto> getRecentOrders(UUID userId, int limit);
    List<AiPaymentSummaryDto> getRecentPaymentsByUser(UUID userId, int limit);
    Optional<AiPaymentSummaryDto> getPaymentByReference(String reference);
}