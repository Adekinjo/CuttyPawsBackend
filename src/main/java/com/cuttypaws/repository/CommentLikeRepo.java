package com.cuttypaws.repository;

import com.cuttypaws.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepo extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);
    Long countByCommentId(Long commentId);
    void deleteByUserIdAndCommentId(Long userId, Long commentId);
    List<CommentLike> findByUserId(Long userId);
    List<CommentLike> findByCommentId(Long commentId);

    @Query("SELECT cl.reactionType, COUNT(cl) FROM CommentLike cl WHERE cl.comment.id = :commentId GROUP BY cl.reactionType")
    List<Object[]> getReactionCountsByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT COUNT(cl) FROM CommentLike cl WHERE cl.comment.id = :commentId AND cl.reactionType = :reactionType")
    Long countByCommentIdAndReactionType(@Param("commentId") Long commentId, @Param("reactionType") CommentLike.ReactionType reactionType);
}