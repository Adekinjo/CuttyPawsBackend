package com.cuttypaws.mapper;

import com.cuttypaws.dto.NewsletterSubscriberDto;
import com.cuttypaws.entity.NewsletterSubscriber;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class NewsletterMapper {


    public NewsletterSubscriberDto subscribeToEntity(NewsletterSubscriber subscriber) {
        return NewsletterSubscriberDto.builder()
                .id(subscriber.getId())
                .email(subscriber.getEmail())
                .isActive(subscriber.getIsActive())
                .subscribed(subscriber.getSubscribed())
                .createdAt(subscriber.getCreatedAt())
                .build();
    }

    public List<NewsletterSubscriberDto> mapSubscribeToDtoList(List<NewsletterSubscriber> subscribers) {
        return subscribers.stream()
                .map(this::subscribeToEntity)
                .collect(Collectors.toList());
    }
}
