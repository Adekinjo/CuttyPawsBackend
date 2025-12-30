package com.cuttypaws.response;

import com.cuttypaws.dto.PostDto;
import com.cuttypaws.dto.ProductDto;
import com.cuttypaws.dto.UserDto;
import com.cuttypaws.dto.UserStatsDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponse {

    private String accessToken;
    private String refreshToken;
    private boolean requiresVerification;
    private String remainingTime;
    private Integer remainingAttempts;

    public UserResponse(String message, String accessToken) {
        this.message = message;
        this.accessToken = accessToken;
    }
    private List<ProductDto> productList;

    private int status;
    private String message;
    private LocalDateTime timeStamp;
    private List<UserDto> userList;
    private UserDto user;

    private PostDto post;
    private List<PostDto> postList;
    private UserStatsDto userStats;


    private String token;
    private String role;
    private String expirationTime;

}
