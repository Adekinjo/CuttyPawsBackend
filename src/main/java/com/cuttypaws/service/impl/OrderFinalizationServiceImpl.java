package com.cuttypaws.service.impl;

import com.cuttypaws.entity.*;
import com.cuttypaws.enums.OrderStatus;
import com.cuttypaws.exception.InsufficientStockException;
import com.cuttypaws.repository.OrderRepo;
import com.cuttypaws.repository.PaymentRepo;
import com.cuttypaws.repository.ProductRepo;
import com.cuttypaws.service.interf.OrderFinalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderFinalizationServiceImpl implements OrderFinalizationService {

    private final OrderRepo orderRepo;
    private final ProductRepo productRepo;
    private final PaymentRepo paymentRepo;

    @Override
    @Transactional
    public void finalizeOrderFromPayment(Payment payment) {
        if (payment.getOrder() != null) {
            return;
        }

        CheckoutSession session = payment.getCheckoutSession();
        if (session == null) {
            throw new RuntimeException("Checkout session not found for payment");
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(session.getUser());
        order.setSubtotal(session.getSubtotal());
        order.setShippingFee(session.getShippingFee());
        order.setTaxAmount(session.getTaxAmount());
        order.setTotalPrice(session.getTotalAmount());
        order.setCurrency(session.getCurrency());
        order.setShippingAddress(session.getShippingAddress());
        order.setCreatedAt(LocalDateTime.now());
        order.setPaidAt(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.CONFIRMED);

        List<OrderItem> orderItems = new ArrayList<>();

        for (CheckoutSessionItem sessionItem : session.getItems()) {
            Product product = productRepo.findById(sessionItem.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (product.getStock() < sessionItem.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for " + product.getName());
            }

            product.setStock(product.getStock() - sessionItem.getQuantity());
            productRepo.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setUser(session.getUser());
            orderItem.setProduct(product);
            orderItem.setQuantity(sessionItem.getQuantity());
            orderItem.setUnitPrice(sessionItem.getUnitPrice());
            orderItem.setLineTotal(sessionItem.getLineTotal());
            orderItem.setProductName(sessionItem.getProductName());
            orderItem.setProductImageUrl(sessionItem.getProductImageUrl());
            orderItem.setSize(sessionItem.getSize());
            orderItem.setColor(sessionItem.getColor());
            orderItem.setOrderStatus(OrderStatus.CONFIRMED);

            orderItems.add(orderItem);
        }

        order.setOrderItemList(orderItems);

        Order savedOrder = orderRepo.save(order);

        payment.setOrder(savedOrder);
        paymentRepo.save(payment);
    }

    private String generateOrderNumber() {
        return "CP-" + System.currentTimeMillis();
    }
}