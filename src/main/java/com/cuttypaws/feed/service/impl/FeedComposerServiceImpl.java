package com.cuttypaws.feed.service.impl;

import com.cuttypaws.dto.PostDto;
import com.cuttypaws.dto.ProductDto;
import com.cuttypaws.dto.ServiceMediaDto;
import com.cuttypaws.dto.ServiceProfileDto;
import com.cuttypaws.entity.Post;
import com.cuttypaws.entity.Product;
import com.cuttypaws.entity.ProductImage;
import com.cuttypaws.entity.ServiceAdSubscription;
import com.cuttypaws.entity.ServiceMedia;
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
import com.cuttypaws.repository.ServiceMediaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedComposerServiceImpl implements FeedComposerService {

    private final PostRepo postRepo;
    private final ProductRepo productRepo;
    private final ServiceAdSubscriptionRepo serviceAdSubscriptionRepo;
    private final ServiceMediaRepo serviceMediaRepo;
    private final PostMapper postMapper;
    private final PostLikeRepo postLikeRepo;
    private final CommentRepo commentRepo;

    @Override
    @Cacheable(
            value = "mixed-feed",
            condition = "@cacheToggleService.isEnabled()"
    )
    public FeedResponseDto getMixedFeed(
            UUID currentUserId,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 8);
        int fetchSize = safeLimit + 1;

        List<Long> fetchedPostIds = (cursorCreatedAt == null || cursorId == null)
                ? postRepo.fetchFeedIdsFirst(PageRequest.of(0, fetchSize))
                : postRepo.fetchFeedIdsAfter(cursorCreatedAt, cursorId, PageRequest.of(0, fetchSize));

        if (fetchedPostIds == null || fetchedPostIds.isEmpty()) {
            return FeedResponseDto.builder()
                    .items(List.of())
                    .count(0)
                    .generatedAt(LocalDateTime.now())
                    .nextCursorCreatedAt(null)
                    .nextCursorId(null)
                    .hasMore(false)
                    .build();
        }

        List<Post> posts = postRepo.findAllWithOwnerAndMediaByIdIn(fetchedPostIds);

        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, Function.identity(), (a, b) -> a));

        List<Post> orderedPosts = fetchedPostIds.stream()
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

        List<Long> orderedPostIds = orderedPosts.stream()
                .map(Post::getId)
                .toList();

        Map<Long, Integer> likeCountMap = postLikeRepo.countLikesByPostIds(orderedPostIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));

        Map<Long, Integer> commentCountMap = commentRepo.countCommentsByPostIds(orderedPostIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));

        Set<Long> likedPostIds = currentUserId == null
                ? Set.of()
                : new HashSet<>(postLikeRepo.findLikedPostIdsByUserAndPostIds(currentUserId, orderedPostIds));

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

        List<ServiceAdSubscription> allAdCandidates = serviceAdSubscriptionRepo.findFeedAdCandidates(
                LocalDateTime.now(),
                PaymentStatus.PAID,
                PageRequest.of(0, 50)
        );

        List<Product> allProductCandidates = productRepo.findFeedProductCandidates(
                PageRequest.of(0, 50)
        );

        int adStartIndex = 0;
        int productStartIndex = 0;

        if (cursorId != null) {
            if (!allAdCandidates.isEmpty()) {
                adStartIndex = Math.abs(cursorId.intValue()) % allAdCandidates.size();
            }
            if (!allProductCandidates.isEmpty()) {
                productStartIndex = Math.abs(cursorId.intValue()) % allProductCandidates.size();
            }
        }

        List<ServiceAdSubscription> adSubscriptions = rotateAndTake(allAdCandidates, adStartIndex, 6);
        List<Product> candidateProducts = rotateAndTake(allProductCandidates, productStartIndex, 8);

        log.info("allAdCandidates ids = {}",
                allAdCandidates.stream().map(ServiceAdSubscription::getId).toList());
        log.info("selected adSubscriptions ids = {}",
                adSubscriptions.stream().map(ServiceAdSubscription::getId).toList());

        log.info("allProductCandidates ids = {}",
                allProductCandidates.stream().map(Product::getId).toList());
        log.info("selected candidateProducts ids = {}",
                candidateProducts.stream().map(Product::getId).toList());

        List<FeedItemDto> adItems = adSubscriptions.stream()
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

        List<FeedItemDto> productItems = candidateProducts.stream()
                .map(this::mapProductToDto)
                .map(dto -> FeedItemDto.builder()
                        .type(FeedItemType.PRODUCT_RECOMMENDATION)
                        .product(dto)
                        .sponsored(false)
                        .score(scoreProduct(dto))
                        .build())
                .toList();

        BlendResult blendResult = blendFeed(postItems, adItems, productItems, safeLimit);

        FeedItemDto lastDisplayedPostItem = null;
        for (int i = blendResult.items().size() - 1; i >= 0; i--) {
            FeedItemDto item = blendResult.items().get(i);
            if (item.getType() == FeedItemType.POST && item.getPost() != null) {
                lastDisplayedPostItem = item;
                break;
            }
        }

        LocalDateTime nextCursorCreatedAt = null;
        Long nextCursorId = null;

        if (lastDisplayedPostItem != null && lastDisplayedPostItem.getPost() != null) {
            Long lastDisplayedPostId = lastDisplayedPostItem.getPost().getId();

            Post lastDisplayedPost = orderedPosts.stream()
                    .filter(p -> Objects.equals(p.getId(), lastDisplayedPostId))
                    .findFirst()
                    .orElse(null);

            if (lastDisplayedPost != null) {
                nextCursorCreatedAt = lastDisplayedPost.getCreatedAt();
                nextCursorId = lastDisplayedPost.getId();
            }
        }

        boolean hasMore = blendResult.consumedPostCount() < orderedPosts.size();

        log.info("postItems size = {}", postItems.size());
        log.info("adItems size = {}", adItems.size());
        log.info("productItems size = {}", productItems.size());
        log.info("adStartIndex = {}", adStartIndex);
        log.info("productStartIndex = {}", productStartIndex);
        log.info("finalFeed types = {}", blendResult.items().stream()
                .map(item -> item.getType().name())
                .toList());

        return FeedResponseDto.builder()
                .items(blendResult.items())
                .count(blendResult.items().size())
                .generatedAt(LocalDateTime.now())
                .nextCursorCreatedAt(nextCursorCreatedAt)
                .nextCursorId(nextCursorId)
                .hasMore(hasMore)
                .build();
    }

    private <T> List<T> rotateAndTake(List<T> source, int startIndex, int maxCount) {
        if (source == null || source.isEmpty() || maxCount <= 0) {
            return List.of();
        }

        List<T> result = new ArrayList<>();
        int size = source.size();
        int limit = Math.min(maxCount, size);

        for (int i = 0; i < limit; i++) {
            int index = (startIndex + i) % size;
            result.add(source.get(index));
        }

        return result;
    }

    private record BlendResult(
            List<FeedItemDto> items,
            int consumedPostCount
    ) {}

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

        if (Boolean.TRUE.equals(dto.getIsVerified())) {
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

    private BlendResult blendFeed(
            List<FeedItemDto> posts,
            List<FeedItemDto> ads,
            List<FeedItemDto> products,
            int limit
    ) {
        List<FeedItemDto> result = new ArrayList<>();

        int postIndex = 0;
        int adIndex = 0;
        int productIndex = 0;

        while (result.size() < limit && postIndex < posts.size()) {
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

        return new BlendResult(result, postIndex);
    }

    private ServiceProfileDto mapServiceProfileToDto(ServiceProfile serviceProfile) {
        List<ServiceMedia> mediaEntities = serviceMediaRepo.findByServiceProfileIdOrderByDisplayOrderAsc(serviceProfile.getId());

        List<ServiceMediaDto> media = mediaEntities.stream()
                .map(m -> ServiceMediaDto.builder()
                        .id(m.getId())
                        .mediaType(m.getMediaType().name())
                        .mediaUrl(m.getMediaUrl())
                        .isCover(Boolean.TRUE.equals(m.getIsCover()))
                        .build())
                .toList();

        String coverImageUrl = mediaEntities.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsCover()))
                .map(ServiceMedia::getMediaUrl)
                .findFirst()
                .orElseGet(() ->
                        mediaEntities.stream()
                                .map(ServiceMedia::getMediaUrl)
                                .findFirst()
                                .orElse(null)
                );

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
                .media(media)
                .coverImageUrl(coverImageUrl)
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