package com.cuttypaws.repository;

import com.cuttypaws.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepo extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parentComment IS NULL")
    Page<Comment> findTopLevelComments(@Param("postId") Long postId, Pageable pageable);

    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentId);

    Long countByPostId(Long postId);
}
