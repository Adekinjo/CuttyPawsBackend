package com.cuttypaws.entity;

import com.cuttypaws.enums.MediaType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "post_media")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PostMedia extends BaseEntity {

    @Column(nullable = false)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MediaType mediaType;

    // optional: for videos
    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
}
