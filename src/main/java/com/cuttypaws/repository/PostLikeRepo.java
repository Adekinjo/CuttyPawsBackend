package com.cuttypaws.repository;

import com.cuttypaws.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepo extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    Long countByPostId(Long postId);
    void deleteByUserIdAndPostId(Long userId, Long postId);
    List<PostLike> findByUserId(Long userId);
    List<PostLike> findByPostId(Long postId);

    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.owner.id = :userId")
    Long countLikesByUserId(@Param("userId") Long userId);

    // New methods for reactions
    @Query("SELECT pl.reactionType, COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId GROUP BY pl.reactionType")
    List<Object[]> getReactionCountsByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId AND pl.reactionType = :reactionType")
    Long countByPostIdAndReactionType(@Param("postId") Long postId, @Param("reactionType") PostLike.ReactionType reactionType);
}