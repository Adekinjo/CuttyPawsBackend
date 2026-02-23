package com.cuttypaws.entity;

import com.cuttypaws.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "users")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;


    @NotBlank(message = "Name is required")
    private String name;

    @Column(unique = true)
    @NotBlank(message = "Email is required")
    private String email;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "cover image url")
    private String coverImageUrl;

    @Column(name = "password")
    @NotBlank(message = "password is required")
    private  String password;

    @Column(name = "phone_number")
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<OrderItem> orderItemsList;


    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "user")
    @ToString.Exclude
    private Address address;

    @Column(name = "created_at")
    private final LocalDateTime createdAt = LocalDateTime.now();


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products;

    private String companyName;
    private String businessRegistrationNumber;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Post> posts;

    // Add blocked status
    @Column(name = "is_blocked")
    @Builder.Default
    private Boolean isBlocked = false;

    @Column(name = "blocked_reason")
    private String blockedReason;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    // Helper method to check if user is active
    public boolean isActive() {
        return !Boolean.TRUE.equals(isBlocked);
    }

    // Add to User entity
    @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<Follow> following = new ArrayList<>();

    @OneToMany(mappedBy = "following", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<Follow> followers = new ArrayList<>();


}
