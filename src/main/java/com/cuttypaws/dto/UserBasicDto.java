package com.cuttypaws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserBasicDto {
    private Long id;
    private String name;
    private String email;
    private String profileImageUrl;
    private Boolean isBlocked;
    private Boolean isActive;
}

