package com.cuttypaws.entity;

import com.cuttypaws.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private BigDecimal totalPrice;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY,  cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItemList;

    @Column(name = "created_at")
    private  LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING;
}

