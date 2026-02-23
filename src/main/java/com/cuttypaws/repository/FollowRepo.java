package com.cuttypaws.repository;

import com.cuttypaws.entity.Follow;
import com.cuttypaws.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepo extends JpaRepository<Follow, Long> {

    // Check if follow relationship exists
    boolean existsByFollowerAndFollowing(User follower, User following);

    // Find specific follow relationship
    Optional<Follow> findByFollowerAndFollowing(User follower, User following);

    // Get followers count for a user
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.following.id = :userId")
    Long countByFollowingId(@Param("userId") UUID userId);

    // Get following count for a user
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower.id = :userId")
    Long countByFollowerId(@Param("userId") UUID userId);

    // Check if user A follows user B
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END " +
            "FROM Follow f WHERE f.follower.id = :followerId AND f.following.id = :followingId")
    Boolean existsByFollowerIdAndFollowingId(@Param("followerId") UUID followerId,
                                             @Param("followingId") UUID followingId);

    // Get paginated followers list
    @Query("SELECT f FROM Follow f JOIN FETCH f.follower WHERE f.following.id = :userId")
    Page<Follow> findFollowersByUserId(@Param("userId") UUID userId, Pageable pageable);

    // Get paginated following list
    @Query("SELECT f FROM Follow f JOIN FETCH f.following WHERE f.follower.id = :userId")
    Page<Follow> findFollowingByUserId(@Param("userId") UUID userId, Pageable pageable);

    // Get all followers (without pagination)
    @Query("SELECT f FROM Follow f JOIN FETCH f.follower WHERE f.following.id = :userId")
    List<Follow> findAllFollowersByUserId(@Param("userId") UUID userId);

    // Get all following (without pagination)
    @Query("SELECT f FROM Follow f JOIN FETCH f.following WHERE f.follower.id = :userId")
    List<Follow> findAllFollowingByUserId(@Param("userId") UUID userId);

    // Delete follow relationship
    @Modifying
    @Query("DELETE FROM Follow f WHERE f.follower.id = :followerId AND f.following.id = :followingId")
    void deleteByFollowerIdAndFollowingId(@Param("followerId") UUID followerId,
                                          @Param("followingId") UUID followingId);

    // Find muted follows
    @Query("SELECT f FROM Follow f WHERE f.follower.id = :followerId AND f.isMuted = true")
    List<Follow> findMutedUsers(@Param("followerId") UUID followerId);

    // ✅ FIXED: Get only the IDs of followers
    @Query("SELECT f.follower.id FROM Follow f WHERE f.following.id = :userId")
    List<UUID> findFollowerIds(@Param("userId") UUID userId);
}
