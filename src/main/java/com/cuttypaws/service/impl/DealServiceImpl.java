package com.cuttypaws.service.impl;

import com.cuttypaws.dto.*;
import com.cuttypaws.entity.*;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.mapper.DealMapper;
import com.cuttypaws.repository.*;
import com.cuttypaws.response.DealResponse;
import com.cuttypaws.service.interf.DealService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "deals")
public class DealServiceImpl implements DealService {

    private final DealRepo dealRepository;
    private final ProductRepo productRepo;
    private final DealMapper dealMapper;

    @Override
    public DealResponse createDeal(Long productId, DealDto dealDto) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with ID: " + productId));

        Deal deal = new Deal();
        deal.setProduct(product);
        deal.setStartDate(dealDto.getStartDate());
        deal.setEndDate(dealDto.getEndDate());
        deal.setDiscountPercentage(dealDto.getDiscountPercentage());
        deal.setActive(true);

        Deal savedDeal = dealRepository.save(deal);
        return DealResponse.builder()
                .status(200)
                .deal(dealMapper.mapDealToDto(savedDeal))
                .message("Deal created successfully")
                .build();
    }

    @Override
    public DealResponse updateDeal(Long dealId, DealDto dealDto) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new NotFoundException("Deal not found with ID: " + dealId));

        if (dealDto.getStartDate() != null) deal.setStartDate(dealDto.getStartDate());
        if (dealDto.getEndDate() != null) deal.setEndDate(dealDto.getEndDate());
        if (dealDto.getDiscountPercentage() != null)
            deal.setDiscountPercentage(dealDto.getDiscountPercentage());

        Deal updatedDeal = dealRepository.save(deal);
        return DealResponse.builder()
                .status(200)
                .deal(dealMapper.mapDealToDto(updatedDeal))
                .message("Deal updated successfully")
                .build();
    }

    @Override
    public DealResponse deleteDeal(Long dealId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new NotFoundException("Deal not found with ID: " + dealId));

        dealRepository.delete(deal);
        return DealResponse.builder()
                .status(200)
                .message("Deal deleted successfully")
                .build();
    }

    @Override
    @Cacheable("activeDeals")
    public DealResponse getAllActiveDeals() {
        List<Deal> activeDeals = dealRepository.findByActiveTrueAndEndDateAfter(LocalDateTime.now());
        List<DealDto> dealDtos = activeDeals.stream()
                .map(dealMapper::mapDealToDto)
                .collect(Collectors.toList());

        return DealResponse.builder()
                .status(200)
                .dealList(dealDtos)
                .build();
    }

    @Override
    public DealResponse getDealByProductId(Long productId) {
        Deal deal = dealRepository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException("No active deal found for product ID: " + productId));

        return DealResponse.builder()
                .status(200)
                .deal(dealMapper.mapDealToDto(deal))
                .build();
    }

    @Override
    public DealResponse toggleDealStatus(Long dealId, boolean status) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new NotFoundException("Deal not found with ID: " + dealId));

        deal.setActive(status);
        dealRepository.save(deal);

        return DealResponse.builder()
                .status(200)
                .message("Deal status updated to " + status)
                .build();
    }
}