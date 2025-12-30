package com.cuttypaws.mapper;

import com.cuttypaws.dto.CustomerSupportDto;
import com.cuttypaws.entity.CustomerSupport;
import org.springframework.stereotype.Component;

@Component
public class CustomerSupportMapper {


    public static CustomerSupportDto toDto(CustomerSupport customerSupport) {
        CustomerSupportDto dto = new CustomerSupportDto();
        dto.setId(customerSupport.getId());
        dto.setCustomerId(customerSupport.getCustomerId()); // Add this line
        dto.setCustomerName(customerSupport.getCustomerName()); // Add this line
        dto.setEmail(customerSupport.getEmail());
        dto.setSubject(customerSupport.getSubject());
        dto.setMessage(customerSupport.getMessage());
        dto.setCreatedAt(customerSupport.getCreatedAt());
        dto.setResolved(customerSupport.isResolved());
        dto.setStatus(customerSupport.getStatus()); // Add this line
        return dto;
    }

    public static CustomerSupport toEntity(CustomerSupportDto dto) {
        CustomerSupport customerSupport = new CustomerSupport();
        customerSupport.setCustomerId(dto.getCustomerId()); // Add this line
        customerSupport.setCustomerName(dto.getCustomerName()); // Add this line
        customerSupport.setEmail(dto.getEmail());
        customerSupport.setSubject(dto.getSubject());
        customerSupport.setMessage(dto.getMessage());
        customerSupport.setCreatedAt(dto.getCreatedAt());
        customerSupport.setResolved(dto.isResolved());
        customerSupport.setStatus(dto.getStatus()); // Add this line
        return customerSupport;
    }
}
