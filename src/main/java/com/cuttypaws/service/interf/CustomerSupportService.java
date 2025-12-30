package com.cuttypaws.service.interf;

import com.cuttypaws.dto.*;

import java.util.List;

// Service: CustomerSupportService
public interface CustomerSupportService {
    CustomerSupportDto updateComplaintStatus(Long id, String status);
    CustomerSupportDto createInquiry(CustomerSupportDto inquiryDto);
    List<CustomerSupportDto> getAllInquiries();
    CustomerSupportDto getInquiryById(Long id);
    void deleteInquiry(Long id);
}

