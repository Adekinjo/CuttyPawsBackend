package com.cuttypaws.feed.dto;

import com.cuttypaws.dto.PostDto;
import com.cuttypaws.dto.ProductDto;
import com.cuttypaws.dto.ServiceProfileDto;
import com.cuttypaws.feed.enums.FeedItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedItemDto {
    private FeedItemType type;
    private Double score;

    private PostDto post;
    private ProductDto product;
    private ServiceProfileDto serviceAd;

    private Boolean sponsored;
}