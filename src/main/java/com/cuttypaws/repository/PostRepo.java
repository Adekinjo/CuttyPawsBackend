package com.cuttypaws.repository;

import com.cuttypaws.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepo extends JpaRepository<Post, Long> {

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


    @Query("""
   SELECT DISTINCT p FROM Post p
   LEFT JOIN FETCH p.owner
   LEFT JOIN FETCH p.media
   ORDER BY p.createdAt DESC
""")
    List<Post> findAllWithOwnerAndMedia();

    @Query("""
    SELECT DISTINCT p
    FROM Post p
    LEFT JOIN FETCH p.owner
    LEFT JOIN FETCH p.media
    WHERE (:cursorCreatedAt IS NULL)
       OR (p.createdAt < :cursorCreatedAt)
       OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId)
    ORDER BY p.createdAt DESC, p.id DESC
""")
    List<Post> fetchFeedCursor(
            @Param("cursorCreatedAt") java.time.Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            org.springframework.data.domain.Pageable pageable
    );


        // First page (no cursor)
        @Query("""
        SELECT p.id
        FROM Post p
        ORDER BY p.createdAt DESC, p.id DESC
    """)
        List<Long> fetchFeedIdsFirst(Pageable pageable);

        // Next page (cursor provided)
        @Query("""
        SELECT p.id
        FROM Post p
        WHERE (p.createdAt < :cursorCreatedAt)
           OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId)
        ORDER BY p.createdAt DESC, p.id DESC
    """)
        List<Long> fetchFeedIdsAfter(
                @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
                @Param("cursorId") Long cursorId,
                Pageable pageable
        );

        // Step 2 — fetch full posts with relations for those IDs
        @Query("""
        SELECT DISTINCT p
        FROM Post p
        LEFT JOIN FETCH p.owner
        LEFT JOIN FETCH p.media
        WHERE p.id IN :ids
    """)
        List<Post> findAllWithOwnerAndMediaByIdIn(@Param("ids") List<Long> ids);

}