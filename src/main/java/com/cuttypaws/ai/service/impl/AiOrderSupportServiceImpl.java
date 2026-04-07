package com.cuttypaws.ai.service.impl;

import com.cuttypaws.ai.dto.AiOrderSummaryDto;
import com.cuttypaws.ai.dto.AiPaymentSummaryDto;
import com.cuttypaws.ai.service.interf.AiOrderSupportService;
import com.cuttypaws.entity.Order;
import com.cuttypaws.entity.Payment;
import com.cuttypaws.repository.OrderRepo;
import com.cuttypaws.repository.PaymentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiOrderSupportServiceImpl implements AiOrderSupportService {

    private final OrderRepo orderRepo;
    private final PaymentRepo paymentRepo;

    @Override
    public List<AiOrderSummaryDto> getRecentOrders(UUID userId, int limit) {
        return orderRepo.findByUserId(userId, PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(this::mapOrder)
                .toList();
    }

    @Override
    public List<AiPaymentSummaryDto> getRecentPaymentsByUser(UUID userId, int limit) {
        return paymentRepo.findByUserIdOrderByIdDesc(userId, PageRequest.of(0, limit))
                .stream()
                .map(this::mapPayment)
                .toList();
    }

    @Override
    public Optional<AiPaymentSummaryDto> getPaymentByReference(String reference) {
        return paymentRepo.findByReference(reference).map(this::mapPayment);
    }

    private AiOrderSummaryDto mapOrder(Order order) {
        return AiOrderSummaryDto.builder()
                .orderId(order.getId())
                .totalPrice(order.getTotalPrice())
                .createdAt(order.getCreatedAt())
                .itemCount(order.getOrderItemList() != null ? order.getOrderItemList().size() : 0)
                .build();
    }

    private AiPaymentSummaryDto mapPayment(Payment payment) {
        return AiPaymentSummaryDto.builder()
                .paymentId(payment.getId())
                .reference(payment.getReference())
                .transactionId(payment.getTransactionId())
                .paymentIntentId(payment.getPaymentIntentId())
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .currency(payment.getCurrency())
                .amount(payment.getAmount())
                .paymentPurpose(payment.getPaymentPurpose() != null ? payment.getPaymentPurpose().name() : null)
                .build();
    }
}