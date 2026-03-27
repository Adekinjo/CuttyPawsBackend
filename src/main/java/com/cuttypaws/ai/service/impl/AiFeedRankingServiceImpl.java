package com.cuttypaws.ai.service.impl;

import com.cuttypaws.ai.dto.FeedRankingRequestDto;
import com.cuttypaws.ai.dto.FeedRankingResultDto;
import com.cuttypaws.ai.service.interf.AiFeedRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiFeedRankingServiceImpl implements AiFeedRankingService {

    private final ChatClient chatClient;

    @Override
    public List<FeedRankingResultDto> rankFeed(FeedRankingRequestDto request) {
        try {
            BeanOutputConverter<FeedRankingResultDtoList> converter =
                    new BeanOutputConverter<>(FeedRankingResultDtoList.class);

            String content = chatClient.prompt()
                    .system("""
                            You rank CuttyPaws feed candidates for a user.

                            Return JSON only.

                            Rules:
                            - Higher score means more relevant for the user.
                            - Consider pet type, location, recent searches, liked categories,
                              and relevance of posts, service ads, and products.
                            - Do not remove items.
                            - Do not apply hard ad business rules.
                            """)
                    .user("""
                            User ID: %s
                            User city: %s
                            User state: %s
                            Primary pet type: %s
                            Recent searches: %s
                            Liked categories: %s

                            Candidates:
                            %s

                            %s
                            """.formatted(
                            request.getUserId(),
                            request.getUserCity(),
                            request.getUserState(),
                            request.getPrimaryPetType(),
                            request.getRecentSearches(),
                            request.getLikedCategories(),
                            request.getCandidates(),
                            converter.getFormat()
                    ))
                    .call()
                    .content();

            FeedRankingResultDtoList result = converter.convert(content);
            return result != null && result.getItems() != null ? result.getItems() : Collections.emptyList();

        } catch (Exception e) {
            log.error("AI feed ranking failed", e);
            return Collections.emptyList();
        }
    }

    @lombok.Data
    public static class FeedRankingResultDtoList {
        private List<FeedRankingResultDto> items;
    }
}