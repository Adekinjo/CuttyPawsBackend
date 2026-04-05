package com.cuttypaws.repository;

import com.cuttypaws.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostLikeRepo extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByUserIdAndPostId(UUID userId, Long postId);
    boolean existsByUserIdAndPostId(UUID userId, Long postId);
    Long countByPostId(Long postId);
    List<PostLike> findByUserId(UUID userId);

    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.owner.id = :userId")
    Long countLikesByUserId(@Param("userId") UUID userId);

    // New methods for reactions
    @Query("SELECT pl.reactionType, COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId GROUP BY pl.reactionType")
    List<Object[]> getReactionCountsByPostId(@Param("postId") Long postId);

    @Query("""
  SELECT pl.post.id, COUNT(pl)
  FROM PostLike pl
  WHERE pl.post.id IN :postIds
  GROUP BY pl.post.id
""")
    List<Object[]> countLikesByPostIds(@Param("postIds") List<Long> postIds);

    @Query("""
        SELECT pl.post.id
        FROM PostLike pl
        WHERE pl.user.id = :userId
          AND pl.post.id IN :postIds
    """)
    List<Long> findLikedPostIdsByUserAndPostIds(
            @Param("userId") UUID userId,
            @Param("postIds") List<Long> postIds
    );
}