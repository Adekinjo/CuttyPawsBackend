package com.cuttypaws.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "pets")
public class Pet extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type; // Dog, Cat, Bird, Reptile etc.

    private String breed;

    @Column(name = "normalized_breed")
    private String normalizedBreed;

    private Integer age;

    private String gender;

    @Column(length = 1000)
    private String description;

    private String size; // Small, Medium, Large
    private String color;

    @Column(name = "activity_level")
    private String activityLevel; // Low, Medium, High

    private String temperament; // Calm, Playful, Friendly etc.

    private Boolean vaccinated;
    private Boolean neutered;

    @Column(name = "special_needs", length = 500)
    private String specialNeeds;

    private String city;
    private String state;
    private String country;

    @Builder.Default
    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Builder.Default
    @Column(name = "favorite_count")
    private Long favoriteCount = 0L;

    @Builder.Default
    @Column(name = "like_count")
    private Long likeCount = 0L;

    @Builder.Default
    @Column(name = "comment_count")
    private Long commentCount = 0L;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @ElementCollection
    @CollectionTable(name = "pet_tags", joinColumns = @JoinColumn(name = "pet_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Pet owner

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PetImage> images = new ArrayList<>();
}