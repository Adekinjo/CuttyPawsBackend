package com.cuttypaws.repository;

import com.cuttypaws.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepo extends JpaRepository<Post, Long> {

    List<Post> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    List<Post> findAllByOrderByCreatedAtDesc();

    // Optional: For future pagination support
    @Query("SELECT COUNT(p) FROM Post p WHERE p.owner.id = :userId")
    Long countByOwnerId(@Param("userId") UUID userId);

    @Query("""
       SELECT p FROM Post p
       LEFT JOIN FETCH p.likes
       LEFT JOIN FETCH p.media
       WHERE p.id = :postId
       """)
    Optional<Post> findByIdWithLikesAndMedia(@Param("postId") Long postId);

    @Query("""
   SELECT DISTINCT p FROM Post p
   LEFT JOIN FETCH p.likes
   LEFT JOIN FETCH p.media
   WHERE p.owner.id = :userId
   ORDER BY p.createdAt DESC
   """)
    List<Post> findByOwnerIdWithLikesAndMedia(@Param("userId") UUID userId);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.likes ORDER BY p.createdAt DESC")
    List<Post> findAllWithLikes();

    @Query("""
   SELECT DISTINCT p FROM Post p
   LEFT JOIN FETCH p.owner
   LEFT JOIN FETCH p.media
   LEFT JOIN FETCH p.likes
   ORDER BY p.createdAt DESC
""")
    List<Post> findAllWithOwnerLikesMedia();

}