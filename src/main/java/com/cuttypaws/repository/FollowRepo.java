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

@Repository
public interface FollowRepo extends JpaRepository<Follow, Long> {

    // Check if follow relationship exists
    boolean existsByFollowerAndFollowing(User follower, User following);

    // Find specific follow relationship
    Optional<Follow> findByFollowerAndFollowing(User follower, User following);

    // Get followers count for a user
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.following.id = :userId")
    Long countByFollowingId(@Param("userId") Long userId);

    // Get following count for a user
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower.id = :userId")
    Long countByFollowerId(@Param("userId") Long userId);

    // Check if user A follows user B
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END " +
            "FROM Follow f WHERE f.follower.id = :followerId AND f.following.id = :followingId")
    Boolean existsByFollowerIdAndFollowingId(@Param("followerId") Long followerId,
                                             @Param("followingId") Long followingId);

    // Get paginated followers list
    @Query("SELECT f FROM Follow f JOIN FETCH f.follower WHERE f.following.id = :userId")
    Page<Follow> findFollowersByUserId(@Param("userId") Long userId, Pageable pageable);

    // Get paginated following list
    @Query("SELECT f FROM Follow f JOIN FETCH f.following WHERE f.follower.id = :userId")
    Page<Follow> findFollowingByUserId(@Param("userId") Long userId, Pageable pageable);

    // Get all followers (without pagination)
    @Query("SELECT f FROM Follow f JOIN FETCH f.follower WHERE f.following.id = :userId")
    List<Follow> findAllFollowersByUserId(@Param("userId") Long userId);

    // Get all following (without pagination)
    @Query("SELECT f FROM Follow f JOIN FETCH f.following WHERE f.follower.id = :userId")
    List<Follow> findAllFollowingByUserId(@Param("userId") Long userId);

    // Delete follow relationship
    @Modifying
    @Query("DELETE FROM Follow f WHERE f.follower.id = :followerId AND f.following.id = :followingId")
    void deleteByFollowerIdAndFollowingId(@Param("followerId") Long followerId,
                                          @Param("followingId") Long followingId);

    // Find muted follows
    @Query("SELECT f FROM Follow f WHERE f.follower.id = :followerId AND f.isMuted = true")
    List<Follow> findMutedUsers(@Param("followerId") Long followerId);

    // ✅ FIXED: Get only the IDs of followers
    @Query("SELECT f.follower.id FROM Follow f WHERE f.following.id = :userId")
    List<Long> findFollowerIds(@Param("userId") Long userId);
}
