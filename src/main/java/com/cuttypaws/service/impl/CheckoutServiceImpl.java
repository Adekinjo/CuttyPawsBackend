package com.cuttypaws.service.impl;

import com.cuttypaws.dto.CreateCheckoutSessionRequest;
import com.cuttypaws.dto.OrderItemDto;
import com.cuttypaws.dto.OrderItemRequest;
import com.cuttypaws.entity.*;
import com.cuttypaws.exception.InsufficientStockException;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.repository.CheckoutSessionRepo;
import com.cuttypaws.repository.ProductRepo;
import com.cuttypaws.response.CheckoutSessionResponse;
import com.cuttypaws.service.interf.CheckoutService;
import com.cuttypaws.service.interf.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final CheckoutSessionRepo checkoutSessionRepo;
    private final ProductRepo productRepo;
    private final UserService userService;

    @Override
    @Transactional
    public CheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request) {
        User user = userService.getLoginUser();

        if (user.getAddress() == null) {
            throw new IllegalArgumentException("Shipping address is required");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart items are required");
        }

        List<CheckoutSessionItem> sessionItems = new ArrayList<>();
        List<OrderItemDto> itemDtos = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepo.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found"));

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for " + product.getName());
            }

            BigDecimal unitPrice = product.getNewPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            CheckoutSessionItem sessionItem = new CheckoutSessionItem();
            sessionItem.setProduct(product);
            sessionItem.setQuantity(itemRequest.getQuantity());
            sessionItem.setUnitPrice(unitPrice);
            sessionItem.setLineTotal(lineTotal);
            sessionItem.setProductName(product.getName());
            sessionItem.setProductImageUrl(
                    product.getImages() != null && !product.getImages().isEmpty()
                            ? product.getImages().get(0).getImageUrl()
                            : null
            );
            sessionItem.setSize(itemRequest.getSize());
            sessionItem.setColor(itemRequest.getColor());

            sessionItems.add(sessionItem);

            itemDtos.add(
                    OrderItemDto.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .productImageUrl(sessionItem.getProductImageUrl())
                            .quantity(itemRequest.getQuantity())
                            .unitPrice(unitPrice)
                            .lineSubtotal(lineTotal)
                            .selectedSize(itemRequest.getSize())
                            .selectedColor(itemRequest.getColor())
                            .build()
            );

            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal shippingFee = calculateShipping(subtotal);
        BigDecimal taxAmount = calculateTax(subtotal, user);
        BigDecimal totalAmount = subtotal.add(shippingFee).add(taxAmount);

        CheckoutSession session = new CheckoutSession();
        session.setUser(user);
        session.setSubtotal(subtotal);
        session.setShippingFee(shippingFee);
        session.setTaxAmount(taxAmount);
        session.setTotalAmount(totalAmount);
        session.setCurrency("USD");
        session.setShippingAddress(formatAddress(user.getAddress()));

        for (CheckoutSessionItem sessionItem : sessionItems) {
            sessionItem.setCheckoutSession(session);
        }
        session.setItems(sessionItems);

        CheckoutSession savedSession = checkoutSessionRepo.save(session);

        return CheckoutSessionResponse.builder()
                .status(200)
                .message("Checkout session created successfully")
                .checkoutSessionId(savedSession.getId())
                .subtotal(savedSession.getSubtotal())
                .shippingFee(savedSession.getShippingFee())
                .taxAmount(savedSession.getTaxAmount())
                .totalAmount(savedSession.getTotalAmount())
                .currency(savedSession.getCurrency())
                .items(itemDtos)
                .build();
    }

    private BigDecimal calculateShipping(BigDecimal subtotal) {
        if (subtotal.compareTo(new BigDecimal("75.00")) >= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal("6.99").setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTax(BigDecimal subtotal, User user) {
        String state = user.getAddress().getState();

        BigDecimal taxRate = switch (state == null ? "" : state.trim().toUpperCase()) {
            case "AL", "ALABAMA" -> new BigDecimal("0.04");
            default -> BigDecimal.ZERO;
        };

        return subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatAddress(Address address) {
        return address.getStreet() + ", " +
                address.getCity() + ", " +
                address.getState() + " " +
                address.getZipcode() + ", " +
                address.getCountry();
    }
}