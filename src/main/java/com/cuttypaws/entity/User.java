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

@Getter
@Setter
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
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "Phone number is required")
    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    @Builder.Default
    private UserRole userRole = UserRole.ROLE_USER;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "user")
    @ToString.Exclude
    private Address address;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<OrderItem> orderItemsList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Product> products = new ArrayList<>();

    @Column(name = "is_service_provider", nullable = false)
    @Builder.Default
    private Boolean isServiceProvider = false;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private ServiceProfile serviceProfile;

    @Column(name = "pets_count", nullable = false)
    @Builder.Default
    private Integer petsCount = 0;

    @Builder.Default
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Post> posts = new ArrayList<>();

    private String companyName;
    private String businessRegistrationNumber;

    @Column(name = "is_blocked", nullable = false)
    @Builder.Default
    private Boolean isBlocked = false;

    @Column(name = "blocked_reason")
    private String blockedReason;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<Follow> following = new ArrayList<>();

    @OneToMany(mappedBy = "following", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<Follow> followers = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isActive() {
        return !Boolean.TRUE.equals(isBlocked);
    }
}