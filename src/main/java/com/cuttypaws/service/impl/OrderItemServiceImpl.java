
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
    private final PaymentService paymentService;


    @Transactional
    @Override
    public OrderResponse createOrderAfterPayment(PaymentOrderRequest request) {
        try {
            log.info("Creating order after successful payment for payment ID: {}", request.getPaymentId());

            // Verify payment exists and is successful
            Payment payment = paymentRepo.findById(request.getPaymentId())
                    .orElseThrow(() -> {
                        log.error("Payment not found with ID: {}", request.getPaymentId());
                        return new NotFoundException("Payment not found");
                    });

            if (payment.getStatus() != PaymentStatus.SUCCESS) {
                log.error("Payment is not successful. Payment status: {}", payment.getStatus());
                throw new IllegalArgumentException("Payment is not successful. Please complete payment first.");
            }

            if (payment.getOrder() != null) {
                log.error("Order already exists for this payment. Order ID: {}", payment.getOrder().getId());
                throw new IllegalArgumentException("Order already created for this payment");
            }

            User user = payment.getUser();

            // Validate order items
            if (request.getItems() == null || request.getItems().isEmpty()) {
                log.error("Order items are null or empty.");
                throw new IllegalArgumentException("Order items cannot be null or empty");
            }

            // Create order items and update stock
            List<OrderItem> orderItems = request.getItems().stream().map(orderItemRequest -> {
                Product product = productRepo.findById(orderItemRequest.getProductId())
                        .orElseThrow(() -> {
                            log.error("Product not found with ID: {}", orderItemRequest.getProductId());
                            return new NotFoundException("Product Not Found");
                        });

                // Check stock
                if (product.getStock() < orderItemRequest.getQuantity()) {
                    log.error("Insufficient stock for product: {}. Requested: {}, Available: {}",
                            product.getName(), orderItemRequest.getQuantity(), product.getStock());
                    throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
                }

                // Update stock
                product.setStock(product.getStock() - orderItemRequest.getQuantity());
                productRepo.save(product);

                // Create order item
                OrderItem orderItem = new OrderItem();
                orderItem.setSize(orderItemRequest.getSize());
                orderItem.setColor(orderItemRequest.getColor());
                orderItem.setProduct(product);
                orderItem.setQuantity(orderItemRequest.getQuantity());
                orderItem.setPrice(product.getNewPrice().multiply(BigDecimal.valueOf(orderItemRequest.getQuantity())));
                orderItem.setOrderStatus(OrderStatus.CONFIRMED);
                orderItem.setUser(user);
                return orderItem;
            }).collect(Collectors.toList());

            // Calculate total price
            BigDecimal totalPrice = orderItems.stream()
                    .map(OrderItem::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Create order
            Order order = new Order();
            order.setUser(user);
            order.setOrderItemList(orderItems);
            order.setTotalPrice(totalPrice);
            order.setOrderStatus(OrderStatus.CONFIRMED);
            order.setCreatedAt(LocalDateTime.now());

            // Link order items to order
            orderItems.forEach(orderItem -> orderItem.setOrder(order));

            // Save order
            Order savedOrder = orderRepo.save(order);

            // Link payment to order
            payment.setOrder(savedOrder);
            paymentRepo.save(payment);

            log.info("Order created successfully with ID: {} for payment ID: {}", savedOrder.getId(), payment.getId());

            // Send notifications
            sendOrderNotifications(savedOrder);

            return OrderResponse.builder()
                    .status(200)
                    .message("Order created successfully")
                    .orderId(savedOrder.getId())
                    .paymentId(payment.getId())
                    .build();

        } catch (NotFoundException | IllegalArgumentException | InsufficientStockException e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating order: {}", e.getMessage(), e);
            throw new RuntimeException("An unexpected error occurred. Please try again.", e);
        }
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
                if (company != null && company.getUserRole() == UserRole.ROLE_COMPANY) {
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
                    + "Total Price: NGN" + order.getTotalPrice() + "\n"
                    + "Delivery Address: " + user.getAddress().getStreet() + ", "
                    + user.getAddress().getCity() + ", "
                    + user.getAddress().getState() + " "
                    + user.getAddress().getZipcode() + ", "
                    + user.getAddress().getCountry() + "\n\n"
                    + "You will receive another email once your order has been shipped.\n\n"
                    + "Best regards,\n"
                    + "The Kinjomarket Team";

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
                    + "Total Price: NGN" + order.getTotalPrice() + "\n"
                    + "Delivery Address: " + user.getAddress().getStreet() + ", "
                    + user.getAddress().getCity() + ", "
                    + user.getAddress().getState() + " "
                    + user.getAddress().getZipcode() + ", "
                    + user.getAddress().getCountry() + "\n\n"
                    + "Order Items:\n";

            for (OrderItem orderItem : order.getOrderItemList()) {
                body += "Product: " + orderItem.getProduct().getName() + ", Quantity: "
                        + orderItem.getQuantity() + ", Price: NGN"
                        + orderItem.getPrice() + "\n";
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
                    + "Price: NGN" + orderItem.getPrice() + "\n"
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

            if (orderItemPage.isEmpty()) {
                log.error("No orders found with the specified filters.");
                throw new NotFoundException("No Order Found");
            }

            List<OrderItemDto> orderItemDtos = orderItemPage.getContent().stream()
                    .map(orderMapper::mapOrderItemToDtoPlusProductAndUser)
                    .collect(Collectors.toList());

            return OrderResponse.builder()
                    .status(200)
                    .orderItemList(orderItemDtos)
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

    @Override
    public OrderResponse getCompanyProductOrders(Long companyId, Pageable pageable) {
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
