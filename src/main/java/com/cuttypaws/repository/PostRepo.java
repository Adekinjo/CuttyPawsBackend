package com.cuttypaws.repository;

import com.cuttypaws.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepo extends JpaRepository<Post, Long> {

    List<Post> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    List<Post> findAllByOrderByCreatedAtDesc();

    // Optional: For future pagination support
    @Query("SELECT COUNT(p) FROM Post p WHERE p.owner.id = :userId")
    Long countByOwnerId(@Param("userId") Long userId);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.likes LEFT JOIN FETCH p.images WHERE p.id = :postId")
    Optional<Post> findByIdWithLikesAndImages(@Param("postId") Long postId);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.likes WHERE p.owner.id = :userId ORDER BY p.createdAt DESC")
    List<Post> findByOwnerIdWithLikes(@Param("userId") Long userId);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.likes ORDER BY p.createdAt DESC")
    List<Post> findAllWithLikes();


}
