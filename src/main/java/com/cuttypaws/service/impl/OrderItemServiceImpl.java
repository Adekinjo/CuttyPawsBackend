
package com.cuttypaws.service.impl;

import com.cuttypaws.dto.*;
import com.cuttypaws.entity.*;
import com.cuttypaws.enums.*;
import com.cuttypaws.exception.*;
import com.cuttypaws.mapper.OrderMapper;
import com.cuttypaws.repository.*;
import com.cuttypaws.response.OrderResponse;
import com.cuttypaws.service.EmailService;
import com.cuttypaws.service.interf.*;
import com.cuttypaws.specification.OrderItemSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderRepo orderRepo;
    private final OrderItemRepo orderItemRepo;
    private final ProductRepo productRepo;
    private final PaymentRepo paymentRepo;
    private final UserService userService;
    private final OrderMapper orderMapper;
    private final EmailService emailService;


    @Override
    public OrderResponse getPaymentResult(String reference) {
        Payment payment = paymentRepo.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.PAID) {
            return OrderResponse.builder()
                    .status(202)
                    .message("Payment is still pending")
                    .paymentId(payment.getId())
                    .build();
        }

        if (payment.getOrder() == null) {
            return OrderResponse.builder()
                    .status(202)
                    .message("Payment succeeded but order is still being finalized")
                    .paymentId(payment.getId())
                    .build();
        }

        Order order = payment.getOrder();

        return OrderResponse.builder()
                .status(200)
                .message("Order retrieved successfully")
                .paymentId(payment.getId())
                .orderId(order.getId())
                .order(orderMapper.toOrderDto(order))
                .build();
    }

    // Method to send email notifications
    private void sendOrderNotifications(Order order) {
        try {
            // Send confirmation email to the user
            sendCustomerOrderConfirmation(order.getUser(), order);

            // Send notification to the admin
            sendAdminOrderNotification(order.getUser(), order);

            // Send notification to the company (if applicable)
            for (OrderItem orderItem : order.getOrderItemList()) {
                User company = orderItem.getProduct().getUser(); // Assuming the product has a reference to the company/user who added it
                if (company != null && company.getUserRole() == UserRole.ROLE_SELLER) {
                    sendCompanyOrderNotification(company, order, orderItem);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send email notifications: {}", e.getMessage(), e);
        }
    }

    // Method to send confirmation email to the customer
    private void sendCustomerOrderConfirmation(User user, Order order) {
        try {
            String subject = "Order Confirmation";
            String body = "Dear " + user.getName() + ",\n\n"
                    + "Thank you for your order. Here are your order details:\n\n"
                    + "Order ID: " + order.getId() + "\n"
                    + "Total Price: USD" + order.getTotalPrice() + "\n"
                    + "Delivery Address: " + user.getAddress().getStreet() + ", "
                    + user.getAddress().getCity() + ", "
                    + user.getAddress().getState() + " "
                    + user.getAddress().getZipcode() + ", "
                    + user.getAddress().getCountry() + "\n\n"
                    + "You will receive another email once your order has been shipped.\n\n"
                    + "Best regards,\n"
                    + "The CuttyPaws Team";

            emailService.sendEmail(user.getEmail(), subject, body);
            log.info("Order confirmation email sent to user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email to user: {}", user.getEmail(), e);
        }
    }

    // Method to send the order notification to the admin
    private void sendAdminOrderNotification(User user, Order order) {
        try {
            String adminEmail = "kinjomarketmessage@gmail.com";
            String subject = "New Order Placed - Order ID " + order.getId();
            String body = "Hello Admin,\n\n"
                    + "A new order has been placed by " + user.getName() + ". Here are the order details:\n\n"
                    + "Customer Name: " + user.getName() + "\n"
                    + "Customer Email: " + user.getEmail() + "\n"
                    + "Order ID: " + order.getId() + "\n"
                    + "Total Price: USD" + order.getTotalPrice() + "\n"
                    + "Delivery Address: " + user.getAddress().getStreet() + ", "
                    + user.getAddress().getCity() + ", "
                    + user.getAddress().getState() + " "
                    + user.getAddress().getZipcode() + ", "
                    + user.getAddress().getCountry() + "\n\n"
                    + "Order Items:\n";

            for (OrderItem orderItem : order.getOrderItemList()) {
                body += "Product: " + orderItem.getProduct().getName() + ", Quantity: "
                        + orderItem.getQuantity() + ", Price: USD"
                        + orderItem.getUnitPrice() + "\n";
            }

            body += "\nThank you for reviewing this order.";

            emailService.sendEmail(adminEmail, subject, body);
            log.info("Order notification email sent to admin: {}", adminEmail);
        } catch (Exception e) {
            log.error("Failed to send order notification email to admin: {}", e.getMessage());
        }
    }

    // Method to send the order notification to the company
    private void sendCompanyOrderNotification(User company, Order order, OrderItem orderItem) {
        try {
            String subject = "New Order for Your Product - Order ID " + order.getId();
            String body = "Hello " + company.getName() + ",\n\n"
                    + "A new order has been placed for your product. Here are the order details:\n\n"
                    + "Customer Name: " + order.getUser().getName() + "\n"
                    + "Customer Email: " + order.getUser().getEmail() + "\n"
                    + "Order ID: " + order.getId() + "\n"
                    + "Product: " + orderItem.getProduct().getName() + "\n"
                    + "Quantity: " + orderItem.getQuantity() + "\n"
                    + "Price: NGN" + orderItem.getUnitPrice() + "\n"
                    + "Total Order Price: NGN" + order.getTotalPrice() + "\n\n"
                    + "Please prepare the product for shipping.\n\n"
                    + "Best regards,\n"
                    + "The Kinjomarket Team";

            emailService.sendEmail(company.getEmail(), subject, body);
            log.info("Order notification email sent to company: {}", company.getEmail());
        } catch (Exception e) {
            log.error("Failed to send order notification email to company: {}", company.getEmail(), e);
        }
    }

    @Override
    public OrderResponse updateOrderItemStatus(Long orderItemId, String status) {
        try {
            OrderItem orderItem = orderItemRepo.findById(orderItemId)
                    .orElseThrow(() -> {
                        log.error("Order item not found with ID: {}", orderItemId);
                        return new NotFoundException("Order Item not found");
                    });

            orderItem.setOrderStatus(OrderStatus.valueOf(status.toUpperCase()));
            orderItem.setUpdatedAt(LocalDateTime.now());
            orderItemRepo.save(orderItem);
            log.info("Order item status updated successfully for ID: {}", orderItemId);

            return OrderResponse.builder()
                    .status(200)
                    .message("Order status updated successfully")
                    .build();
        } catch (NotFoundException e) {
            log.error("Error updating order item status: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating order item status: {}", e.getMessage(), e);
            throw new RuntimeException("An unexpected error occurred. Please try again.", e);
        }
    }

    @Override
    public OrderResponse filterOrderItems(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate, Long itemId, Pageable pageable) {
        try {
            Specification<OrderItem> spec = Specification.where(OrderItemSpecification.hasStatus(status))
                    .and(OrderItemSpecification.createdBetween(startDate, endDate))
                    .and(OrderItemSpecification.hasItemId(itemId));

            Page<OrderItem> orderItemPage = orderItemRepo.findAll(spec, pageable);

            if (orderItemPage == null) {
                log.error("No orders found with the specified filters.");
                throw new NotFoundException("No Order Found");
            }

            List<OrderItemDto> orderItemDtos = orderItemPage.getContent().stream()
                    .map(orderMapper::mapOrderItemToDtoPlusProductAndUser)
                    .collect(Collectors.toList());

            return OrderResponse.builder()
                    .status(200)
                    .orderItemList(orderItemDtos)
                    .message(orderItemDtos.isEmpty()
                            ? "No orders match the selected filters."
                            : "Orders retrieved successfully.")
                    .totalPage(orderItemPage.getTotalPages())
                    .totalElement(orderItemPage.getTotalElements())
                    .build();
        } catch (NotFoundException e) {
            log.error("Error filtering order items: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error filtering order items: {}", e.getMessage(), e);
            throw new RuntimeException("An unexpected error occurred. Please try again.", e);
        }
    }

    @Transactional
    @Override
    public OrderResponse getMyOrders(Pageable pageable) {
        UUID userId = userService.getLoginUser().getId();

        Page<Order> page = orderRepo.findByUserId(userId, pageable);

        List<OrderItemDto> orderItems = page.getContent().stream()
                .flatMap(order -> order.getOrderItemList().stream())
                .map(orderMapper::toOrderItemDto)
                .toList();

        return OrderResponse.builder()
                .status(200)
                .message("My orders retrieved successfully")
                .orderItemList(orderItems)
                .totalPage(page.getTotalPages())
                .totalElement(page.getTotalElements())
                .build();
    }

    @Override
    public OrderResponse getOrderItemForAdmin(Long itemId) {
        OrderItem orderItem = orderItemRepo.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Order item not found"));

        return OrderResponse.builder()
                .status(200)
                .message("Order item retrieved successfully")
                .orderItem(orderMapper.mapOrderItemToDtoPlusProductAndUser(orderItem))
                .build();
    }

    @Override
    public OrderResponse getMyOrderItemById(Long itemId) {
        UUID userId = userService.getLoginUser().getId();

        OrderItem orderItem = orderItemRepo.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Order item not found"));

        return OrderResponse.builder()
                .status(200)
                .message("Order item retrieved successfully")
                .orderItem(orderMapper.mapOrderItemToDtoPlusProductAndUser(orderItem))
                .build();
    }


    @Override
    public OrderResponse getCompanyProductOrders(UUID companyId, Pageable pageable) {
        try {
            // Fetch all products belonging to the company
            List<Long> productIds = productRepo.findByUserId(companyId).stream()
                    .map(Product::getId)
                    .collect(Collectors.toList());

            if (productIds.isEmpty()) {
                log.error("No products found for company with ID: {}", companyId);
                throw new NotFoundException("No products found for the company");
            }

            // Fetch order items for the products
            Page<OrderItem> orderItemsPage = orderItemRepo.findByProductIdIn(productIds, pageable);

            if (orderItemsPage.isEmpty()) {
                log.error("No orders found for company's products with ID: {}", companyId);
                throw new NotFoundException("No orders found for the company's products");
            }

            // Map order items to DTOs
            List<OrderItemDto> orderItemDtos = orderItemsPage.getContent().stream()
                    .map(orderMapper::mapOrderItemToDtoPlusProductAndUser)
                    .collect(Collectors.toList());

            return OrderResponse.builder()
                    .status(200)
                    .orderItemList(orderItemDtos)
                    .totalPage(orderItemsPage.getTotalPages())
                    .totalElement(orderItemsPage.getTotalElements())
                    .build();
        } catch (NotFoundException e) {
            log.error("Error fetching company product orders: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching company product orders: {}", e.getMessage(), e);
            throw new RuntimeException("An unexpected error occurred. Please try again.", e);
        }
    }
}
