package com.cuttypaws.controller;

import com.cuttypaws.dto.CustomerSupportDto;
import com.cuttypaws.service.interf.CustomerSupportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/support")
public class CustomerSupportController {

    @Autowired
    private CustomerSupportService customerSupportService;

    @PostMapping("/create")
    public ResponseEntity<CustomerSupportDto> createInquiry(@RequestBody CustomerSupportDto inquiryDto) {
        CustomerSupportDto createdInquiry = customerSupportService.createInquiry(inquiryDto);
        return ResponseEntity.ok(createdInquiry);
    }

    @GetMapping("/get-all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<List<CustomerSupportDto>> getAllInquiries() {
        List<CustomerSupportDto> inquiries = customerSupportService.getAllInquiries();
        return ResponseEntity.ok(inquiries);
    }

    @GetMapping("/get-support-by-id/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<CustomerSupportDto> getInquiryById(@PathVariable Long id) {
        CustomerSupportDto inquiry = customerSupportService.getInquiryById(id);
        return ResponseEntity.ok(inquiry);
    }

    @PutMapping("/update-status/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<CustomerSupportDto> updateComplaintStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        CustomerSupportDto updatedInquiry = customerSupportService.updateComplaintStatus(id, status);
        return ResponseEntity.ok(updatedInquiry);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<Void> deleteInquiry(@PathVariable Long id) {
        customerSupportService.deleteInquiry(id);
        return ResponseEntity.noContent().build();
    }
}