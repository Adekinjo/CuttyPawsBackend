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

    private List<MultipartFile> images;
}
