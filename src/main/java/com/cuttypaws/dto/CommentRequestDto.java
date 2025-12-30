package com.cuttypaws.dto;

import lombok.Data;

@Data
public class CommentRequestDto {
    private Long postId;
    private String content;
    private Long parentCommentId; // null = top-level comment
}
