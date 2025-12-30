// DealService.java
package com.cuttypaws.service.interf;

import com.cuttypaws.dto.*;
import com.cuttypaws.response.DealResponse;

public interface DealService {
    DealResponse createDeal(Long productId, DealDto dealDto);
    DealResponse updateDeal(Long dealId, DealDto dealDto);
    DealResponse deleteDeal(Long dealId);
    DealResponse getAllActiveDeals();
    DealResponse getDealByProductId(Long productId);
    DealResponse toggleDealStatus(Long dealId, boolean status);
}