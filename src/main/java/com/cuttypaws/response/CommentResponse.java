package com.cuttypaws.response;

import com.cuttypaws.dto.CommentDto;
import com.cuttypaws.dto.UserStatsDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentResponse {

    private int status;
    private String message;
    private LocalDateTime timeStamp = LocalDateTime.now();
    private List<CommentDto> commentList; // For multiple comments
    private CommentDto comment; // For single comment
    private Integer totalComments;
    private UserStatsDto userStats;
    private Map<String, Long> reactions;  // emoji reactions map
    private Integer currentPage;          // pagination
    private Integer totalPages;           // pagination
    private Object data;                  // dynamic payload (WebSocket or custom)
    private Map<String, Object> extra;    // flexible additional metadata

}
