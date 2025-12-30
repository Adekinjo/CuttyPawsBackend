package com.cuttypaws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {

    private Long id;
    private String content;

    private Long userId;
    private String userName;
    private String userProfileImage;

    private Long postId;
    private Long parentCommentId;

    private Integer likeCount;
    private Boolean isLikedByCurrentUser;

    private String userReaction; // Current user's reaction type

    private Map<String, Long> reactions; // ❤️ 😂 🔥 👍

    private List<CommentDto> replies;    // 1-level replies only

    private String createdAt;
    private String updatedAt;
}
