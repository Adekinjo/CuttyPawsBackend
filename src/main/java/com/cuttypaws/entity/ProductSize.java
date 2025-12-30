package com.cuttypaws.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "product_sizes")
public class ProductSize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String size;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private  String name;

}