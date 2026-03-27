package com.cuttypaws.entity;

import com.cuttypaws.enums.MediaType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_media")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String mediaUrl;

    @Column(length = 1000)
    private String thumbnailUrl;

    @Column(length = 1000)
    private String streamUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType mediaType;

    private Integer durationSeconds;

    @Column(nullable = false)
    @Builder.Default
    private Boolean processed = Boolean.TRUE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
}