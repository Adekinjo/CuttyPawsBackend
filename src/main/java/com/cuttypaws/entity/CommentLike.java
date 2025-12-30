package com.cuttypaws.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "comment_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "comment_id"})
})
public class CommentLike extends BaseEntity {

    public enum ReactionType {
        LIKE, LOVE, HAHA, WOW, SAD, ANGRY
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    @Builder.Default
    private ReactionType reactionType = ReactionType.LIKE;
}