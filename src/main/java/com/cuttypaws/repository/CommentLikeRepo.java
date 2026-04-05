package com.cuttypaws.repository;

import com.cuttypaws.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentLikeRepo extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByUserIdAndCommentId(UUID userId, Long commentId);
    List<CommentLike> findByUserId(UUID userId);

    @Query("SELECT cl.reactionType, COUNT(cl) FROM CommentLike cl WHERE cl.comment.id = :commentId GROUP BY cl.reactionType")
    List<Object[]> getReactionCountsByCommentId(@Param("commentId") Long commentId);

}