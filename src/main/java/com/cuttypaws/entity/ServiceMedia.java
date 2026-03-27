package com.cuttypaws.entity;

import com.cuttypaws.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "service_media")
public class ServiceMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_profile_id")
    private ServiceProfile serviceProfile;

    @Enumerated(EnumType.STRING)
    private MediaType mediaType; // IMAGE or VIDEO

    private String mediaUrl;

    private Boolean isCover;

    private Integer displayOrder;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}