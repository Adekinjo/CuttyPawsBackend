package com.cuttypaws.response;

import com.cuttypaws.dto.PostDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostLikeResponse {

    private int status;
    private String message;

    private LocalDateTime timeStamp;
    private PostDto post;
    private List<PostDto> postList;

    private Map<String, Long> reactions;  // emoji reactions map
    private Integer currentPage;          // pagination
    private Integer totalPages;           // pagination
    private Object data;                  // dynamic payload (WebSocket or custom)
    private Map<String, Object> extra;
}
