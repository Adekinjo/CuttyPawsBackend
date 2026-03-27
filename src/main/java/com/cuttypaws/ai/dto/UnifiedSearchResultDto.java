package com.cuttypaws.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class UnifiedSearchResultDto {
    private List<String> entityTypes;   // PRODUCTS, POSTS, USERS, SERVICES
    private String petType;             // DOG, CAT, BIRD...
    private String serviceType;         // GROOMER, VET, DAYCARE...
    private String city;
    private String state;
    private String urgency;
    private Integer radiusMiles;
    private List<String> keywords;
}