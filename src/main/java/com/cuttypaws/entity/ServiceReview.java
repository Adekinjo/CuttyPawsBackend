package com.cuttypaws.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "service_reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_service_profile_reviewer", columnNames = {"service_profile_id", "reviewer_id"})
        },
        indexes = {
                @Index(name = "idx_service_review_service_profile_id", columnList = "service_profile_id"),
                @Index(name = "idx_service_review_reviewer_id", columnList = "reviewer_id")
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_profile_id", nullable = false)
    private ServiceProfile serviceProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", length = 2000)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}