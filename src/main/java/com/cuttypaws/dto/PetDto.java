package com.cuttypaws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Integer age;
    private String gender;
    private String description;

    private UUID ownerId;
    private String ownerName;

    private List<String> imageUrls;
}
