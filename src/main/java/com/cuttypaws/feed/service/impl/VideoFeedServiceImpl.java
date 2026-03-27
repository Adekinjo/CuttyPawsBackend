package com.cuttypaws.feed.service.impl;

import com.cuttypaws.dto.PostDto;
import com.cuttypaws.entity.Post;
import com.cuttypaws.feed.dto.VideoFeedCursorDto;
import com.cuttypaws.feed.dto.VideoFeedPageResponse;
import com.cuttypaws.feed.service.interf.VideoFeedService;
import com.cuttypaws.mapper.PostMapper;
import com.cuttypaws.repository.CommentRepo;
import com.cuttypaws.repository.PostLikeRepo;
import com.cuttypaws.repository.PostRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoFeedServiceImpl implements VideoFeedService {

    private final PostRepo postRepo;
    private final PostLikeRepo postLikeRepo;
    private final CommentRepo commentRepo;
    private final PostMapper postMapper;

    @Override
    public VideoFeedPageResponse getVideoFeed(
            UUID currentUserId,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 10);

        List<Long> ids = (cursorCreatedAt == null || cursorId == null)
                ? postRepo.fetchVideoFeedIdsFirst(PageRequest.of(0, safeLimit))
                : postRepo.fetchVideoFeedIdsAfter(cursorCreatedAt, cursorId, PageRequest.of(0, safeLimit));

        if (ids.isEmpty()) {
            return VideoFeedPageResponse.builder()
                    .items(List.of())
                    .nextCursor(null)
                    .hasMore(false)
                    .build();
        }

        List<Post> posts = postRepo.findAllWithOwnerAndMediaByIdIn(ids);

        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        List<Post> orderedPosts = ids.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, Integer> likeCountMap = toCountMap(postLikeRepo.countByPostIds(ids));
        Map<Long, Integer> commentCountMap = toCountMap(commentRepo.countByPostIds(ids));

        Set<Long> likedPostIds = currentUserId == null
                ? Set.of()
                : new HashSet<>(postLikeRepo.findLikedPostIdsByUserAndPostIds(currentUserId, ids));

        List<PostDto> items = orderedPosts.stream()
                .map(post -> postMapper.mapPostToDtoFast(
                        post,
                        likeCountMap.getOrDefault(post.getId(), 0),
                        commentCountMap.getOrDefault(post.getId(), 0),
                        likedPostIds.contains(post.getId())
                ))
                .toList();

        Post lastPost = orderedPosts.get(orderedPosts.size() - 1);

        VideoFeedCursorDto nextCursor = VideoFeedCursorDto.builder()
                .cursorCreatedAt(lastPost.getCreatedAt())
                .cursorId(lastPost.getId())
                .build();

        return VideoFeedPageResponse.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(items.size() == safeLimit)
                .build();
    }

    private Map<Long, Integer> toCountMap(List<Object[]> rows) {
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : rows) {
            Long postId = ((Number) row[0]).longValue();
            Integer count = ((Number) row[1]).intValue();
            result.put(postId, count);
        }
        return result;
    }
}