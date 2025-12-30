package com.cuttypaws.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    @Column(name = "image_url")
    private String imageUrl;

    @ToString.Exclude
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubCategory> subCategories;

    @ToString.Exclude
    @JsonIgnore
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Product> productList;


    @Column(name = "created_at")
    private final LocalDateTime createdAt = LocalDateTime.now();

    public void addSubCategory(SubCategory subCategory) {
        subCategories.add(subCategory);
        subCategory.setCategory(this);
    }
}
