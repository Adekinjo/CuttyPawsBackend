package com.cuttypaws.feed.service.impl;

import com.cuttypaws.dto.PostDto;
import com.cuttypaws.dto.ProductDto;
import com.cuttypaws.dto.ServiceProfileDto;
import com.cuttypaws.entity.Post;
import com.cuttypaws.entity.Product;
import com.cuttypaws.entity.ProductImage;
import com.cuttypaws.entity.ServiceAdSubscription;
import com.cuttypaws.entity.ServiceProfile;
import com.cuttypaws.enums.PaymentStatus;
import com.cuttypaws.feed.dto.FeedItemDto;
import com.cuttypaws.feed.dto.FeedResponseDto;
import com.cuttypaws.feed.enums.FeedItemType;
import com.cuttypaws.feed.service.interf.FeedComposerService;
import com.cuttypaws.mapper.PostMapper;
import com.cuttypaws.repository.CommentRepo;
import com.cuttypaws.repository.PostLikeRepo;
import com.cuttypaws.repository.PostRepo;
import com.cuttypaws.repository.ProductRepo;
import com.cuttypaws.repository.ServiceAdSubscriptionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedComposerServiceImpl implements FeedComposerService {

    private final PostRepo postRepo;
    private final ProductRepo productRepo;
    private final ServiceAdSubscriptionRepo serviceAdSubscriptionRepo;
    private final PostMapper postMapper;
    private final PostLikeRepo postLikeRepo;
    private final CommentRepo commentRepo;

    @Override
    @Cacheable(
            value = "mixed-feed",
            key = "T(String).valueOf(#currentUserId).concat(':').concat(T(String).valueOf(#cursorCreatedAt)).concat(':').concat(T(String).valueOf(#cursorId)).concat(':').concat(T(String).valueOf(#limit))",
            condition = "@cacheToggleService.isEnabled()",
            unless = "#result == null || #result.items == null || #result.items.isEmpty()"
    )
    public FeedResponseDto getMixedFeed(
            UUID currentUserId,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 12);
        int fetchSize = safeLimit + 1;

        // 1. Get post ids with cursor pagination
        List<Long> postIds;
        if (cursorCreatedAt == null || cursorId == null) {
            postIds = postRepo.fetchFeedIdsFirst(PageRequest.of(0, fetchSize));
        } else {
            postIds = postRepo.fetchFeedIdsAfter(cursorCreatedAt, cursorId, PageRequest.of(0, fetchSize));
        }

        if (postIds == null || postIds.isEmpty()) {
            return FeedResponseDto.builder()
                    .items(List.of())
                    .count(0)
                    .generatedAt(LocalDateTime.now())
                    .nextCursorCreatedAt(null)
                    .nextCursorId(null)
                    .hasMore(false)
                    .build();
        }

        // 2. Determine hasMore and trim extra row
        boolean hasMore = postIds.size() > safeLimit;
        if (hasMore) {
            postIds = postIds.subList(0, safeLimit);
        }

        // 3. Load full posts with owner + media
        List<Post> posts = postRepo.findAllWithOwnerAndMediaByIdIn(postIds);

        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, Function.identity(), (a, b) -> a));

        // preserve DB id order
        List<Post> orderedPosts = postIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .toList();

        if (orderedPosts.isEmpty()) {
            return FeedResponseDto.builder()
                    .items(List.of())
                    .count(0)
                    .generatedAt(LocalDateTime.now())
                    .nextCursorCreatedAt(null)
                    .nextCursorId(null)
                    .hasMore(false)
                    .build();
        }

        List<Long> orderedPostIds = orderedPosts.stream().map(Post::getId).toList();

        // 4. Batch like counts
        Map<Long, Integer> likeCountMap = postLikeRepo.countLikesByPostIds(orderedPostIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()
                ));

        // 5. Batch comment counts
        Map<Long, Integer> commentCountMap = commentRepo.countCommentsByPostIds(orderedPostIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()
                ));

        // 6. Batch liked-post lookup for current user
        Set<Long> likedPostIds = currentUserId == null
                ? Set.of()
                : new HashSet<>(postLikeRepo.findLikedPostIdsByUserAndPostIds(currentUserId, orderedPostIds));

        // 7. Build post items
        // NOTE:
        // This keeps posts in cursor/date order.
        // If you sort by score again here, you lose strict DESC order.
        List<FeedItemDto> postItems = orderedPosts.stream()
                .map(post -> {
                    int likeCount = likeCountMap.getOrDefault(post.getId(), 0);
                    int commentCount = commentCountMap.getOrDefault(post.getId(), 0);
                    boolean likedByCurrentUser = likedPostIds.contains(post.getId());

                    PostDto postDto = postMapper.mapPostToDtoFast(
                            post,
                            likeCount,
                            commentCount,
                            likedByCurrentUser
                    );

                    return FeedItemDto.builder()
                            .type(FeedItemType.POST)
                            .post(postDto)
                            .sponsored(false)
                            .score(scorePost(post, likeCount, commentCount))
                            .build();
                })
                .toList();

        // 8. Ads
        List<ServiceAdSubscription> activeAds =
                serviceAdSubscriptionRepo.findByIsActiveTrueAndEndsAtAfterAndPaymentStatusOrderByCreatedAtDesc(
                                LocalDateTime.now(),
                                PaymentStatus.PAID
                        )
                        .stream()
                        .limit(2)
                        .toList();

        List<FeedItemDto> adItems = activeAds.stream()
                .map(ServiceAdSubscription::getServiceProfile)
                .filter(Objects::nonNull)
                .map(this::mapServiceProfileToDto)
                .map(dto -> FeedItemDto.builder()
                        .type(FeedItemType.SERVICE_AD)
                        .serviceAd(dto)
                        .sponsored(true)
                        .score(scoreServiceAd(dto))
                        .build())
                .toList();

        // 9. Products
        List<Product> candidateProducts = productRepo.findTop4ByOrderByLikesDesc();

        List<FeedItemDto> productItems = candidateProducts.stream()
                .map(this::mapProductToDto)
                .map(dto -> FeedItemDto.builder()
                        .type(FeedItemType.PRODUCT_RECOMMENDATION)
                        .product(dto)
                        .sponsored(false)
                        .score(scoreProduct(dto))
                        .build())
                .toList();

        // 10. Blend feed
        List<FeedItemDto> finalFeed = blendFeed(postItems, adItems, productItems, safeLimit);

        // 11. Next cursor from last actual post in cursor sequence
        Post lastPost = orderedPosts.get(orderedPosts.size() - 1);

        return FeedResponseDto.builder()
                .items(finalFeed)
                .count(finalFeed.size())
                .generatedAt(LocalDateTime.now())
                .nextCursorCreatedAt(lastPost.getCreatedAt())
                .nextCursorId(lastPost.getId())
                .hasMore(hasMore)
                .build();
    }

    private double scorePost(Post post, int likeCount, int commentCount) {
        double score = 0.0;

        if (post.getCreatedAt() != null) {
            long hoursOld = java.time.Duration.between(post.getCreatedAt(), LocalDateTime.now()).toHours();
            score += Math.max(0, 100 - hoursOld);
        }

        score += likeCount * 2.0;
        score += commentCount * 3.0;

        return score;
    }

    private double scoreServiceAd(ServiceProfileDto dto) {
        double score = 50.0;

        if (dto.getIsVerified() != null && dto.getIsVerified()) {
            score += 20.0;
        }

        if (dto.getAverageRating() != null) {
            score += dto.getAverageRating() * 5.0;
        }

        if (dto.getReviewCount() != null) {
            score += Math.min(dto.getReviewCount(), 20);
        }

        return score;
    }

    private double scoreProduct(ProductDto dto) {
        double score = 0.0;

        if (dto.getLikes() != null) {
            score += dto.getLikes() * 2.0;
        }

        if (dto.getViewCount() != null) {
            score += dto.getViewCount() * 0.5;
        }

        if (dto.getStock() != null && dto.getStock() > 0) {
            score += 10.0;
        }

        return score;
    }

    private List<FeedItemDto> blendFeed(
            List<FeedItemDto> posts,
            List<FeedItemDto> ads,
            List<FeedItemDto> products,
            int limit
    ) {
        List<FeedItemDto> result = new ArrayList<>();

        int postIndex = 0;
        int adIndex = 0;
        int productIndex = 0;

        while (result.size() < limit &&
                (postIndex < posts.size() || adIndex < ads.size() || productIndex < products.size())) {

            for (int i = 0; i < 4 && result.size() < limit && postIndex < posts.size(); i++) {
                result.add(posts.get(postIndex++));
            }

            if (result.size() < limit && adIndex < ads.size()) {
                result.add(ads.get(adIndex++));
            }

            for (int i = 0; i < 2 && result.size() < limit && postIndex < posts.size(); i++) {
                result.add(posts.get(postIndex++));
            }

            if (result.size() < limit && productIndex < products.size()) {
                result.add(products.get(productIndex++));
            }
        }

        return result;
    }

    private ServiceProfileDto mapServiceProfileToDto(ServiceProfile serviceProfile) {
        return ServiceProfileDto.builder()
                .id(serviceProfile.getId())
                .userId(serviceProfile.getUser() != null ? serviceProfile.getUser().getId() : null)
                .ownerName(serviceProfile.getUser() != null ? serviceProfile.getUser().getName() : null)
                .ownerProfileImageUrl(serviceProfile.getUser() != null ? serviceProfile.getUser().getProfileImageUrl() : null)
                .serviceType(serviceProfile.getServiceType())
                .businessName(serviceProfile.getBusinessName())
                .tagline(serviceProfile.getTagline())
                .description(serviceProfile.getDescription())
                .city(serviceProfile.getCity())
                .state(serviceProfile.getState())
                .country(serviceProfile.getCountry())
                .zipcode(serviceProfile.getZipcode())
                .serviceArea(serviceProfile.getServiceArea())
                .addressLine(serviceProfile.getAddressLine())
                .priceFrom(serviceProfile.getPriceFrom())
                .priceTo(serviceProfile.getPriceTo())
                .pricingNote(serviceProfile.getPricingNote())
                .yearsOfExperience(serviceProfile.getYearsOfExperience())
                .websiteUrl(serviceProfile.getWebsiteUrl())
                .whatsappNumber(serviceProfile.getWhatsappNumber())
                .isVerified(serviceProfile.getIsVerified())
                .acceptsHomeVisits(serviceProfile.getAcceptsHomeVisits())
                .offersEmergencyService(serviceProfile.getOffersEmergencyService())
                .status(serviceProfile.getStatus())
                .averageRating(serviceProfile.getAverageRating())
                .reviewCount(serviceProfile.getReviewCount())
                .createdAt(serviceProfile.getCreatedAt())
                .updatedAt(serviceProfile.getUpdatedAt())
                .reviewedAt(serviceProfile.getReviewedAt())
                .approvedAt(serviceProfile.getApprovedAt())
                .sponsored(true)
                .sponsoredPlanType("ACTIVE_AD")
                .build();
    }

    private ProductDto mapProductToDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .oldPrice(product.getOldPrice())
                .newPrice(product.getNewPrice())
                .thumbnailImageUrl(
                        product.getImages() == null || product.getImages().isEmpty()
                                ? null
                                : product.getImages().get(0).getImageUrl()
                )
                .imageUrls(
                        product.getImages() == null
                                ? List.of()
                                : product.getImages().stream()
                                .map(ProductImage::getImageUrl)
                                .collect(Collectors.toList())
                )
                .category(product.getCategory() != null ? product.getCategory().getName() : null)
                .subCategory(product.getSubCategory() != null ? product.getSubCategory().getName() : null)
                .likes(product.getLikes())
                .viewCount(product.getViewCount())
                .stock(product.getStock())
                .userId(product.getUser() != null ? product.getUser().getId() : null)
                .companyName(product.getUser() != null ? product.getUser().getCompanyName() : null)
                .build();
    }
}