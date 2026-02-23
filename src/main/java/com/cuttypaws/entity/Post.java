package com.cuttypaws.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "posts")
public class Post extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String caption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostMedia> media;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<PostLike> likes = new HashSet<>();

    private  Integer CommentCount;

    // Helper method to get like count
    public Integer getLikeCount() {
        return likes != null ? likes.size() : 0;
    }

    // Helper method to check if user liked the post
    public boolean isLikedByUser(Long userId) {
        if (likes == null || userId == null) return false;
        return likes.stream().anyMatch(like ->
                like.getUser() != null && like.getUser().getId().equals(userId));
    }
}

