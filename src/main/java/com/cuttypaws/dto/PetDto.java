package com.cuttypaws.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetDto {

    private Long id;
    private String name;
    private String type;
    private String breed;
    private String normalizedBreed;
    private Integer age;
    private String gender;
    private String description;

    private String size;
    private String color;
    private String activityLevel;
    private String temperament;

    private Boolean vaccinated;
    private Boolean neutered;
    private String specialNeeds;

    private String city;
    private String state;
    private String country;

    private Long viewCount;
    private Long favoriteCount;
    private Long likeCount;
    private Long commentCount;

    private String coverImageUrl;
    private List<String> tags;

    private UUID ownerId;
    private String ownerName;

    private List<String> imageUrls;
}