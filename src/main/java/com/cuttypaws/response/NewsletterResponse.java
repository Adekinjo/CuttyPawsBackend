package com.cuttypaws.response;

import com.cuttypaws.dto.NewsletterSubscriberDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsletterResponse {

    private int status;
    private String message;

    private NewsletterSubscriberDto subscriber;
    private List<NewsletterSubscriberDto> subscribers;

}
