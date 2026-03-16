package com.cuttypaws.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class PetRequestDto {

    private String name;
    private String type;
    private String breed;
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

    private List<String> tags;

    private List<MultipartFile> images;

    private Integer CoverImageIndex;
}