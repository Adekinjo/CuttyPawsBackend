package com.cuttypaws.service.impl;

import com.cuttypaws.dto.*;
import com.cuttypaws.entity.*;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.mapper.CustomerSupportMapper;
import com.cuttypaws.repository.*;
import com.cuttypaws.service.interf.CustomerSupportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerSupportServiceImpl implements CustomerSupportService {

    @Autowired
    private CustomerSupportRepo customerSupportRepository;

    @Override
    public CustomerSupportDto createInquiry(CustomerSupportDto inquiryDto) {
        CustomerSupport inquiry = CustomerSupportMapper.toEntity(inquiryDto);
        inquiry.setCreatedAt(LocalDateTime.now());
        inquiry.setResolved(false);
        inquiry.setStatus("CREATED");
        CustomerSupport savedInquiry = customerSupportRepository.save(inquiry);
        return CustomerSupportMapper.toDto(savedInquiry);
    }

    @Override
    public List<CustomerSupportDto> getAllInquiries() {
        return customerSupportRepository.findAll().stream()
                .map(CustomerSupportMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerSupportDto getInquiryById(Long id) {
        CustomerSupport inquiry = customerSupportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inquiry not found with id: " + id));
        return CustomerSupportMapper.toDto(inquiry);
    }

    @Override
    public CustomerSupportDto updateComplaintStatus(Long id, String status) {
        CustomerSupport inquiry = customerSupportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inquiry not found with id: " + id));
        inquiry.setStatus(status); // Update the status
        CustomerSupport updatedInquiry = customerSupportRepository.save(inquiry);
        return CustomerSupportMapper.toDto(updatedInquiry);
    }

    @Override
    public void deleteInquiry(Long id) {
        customerSupportRepository.deleteById(id);
    }
}
