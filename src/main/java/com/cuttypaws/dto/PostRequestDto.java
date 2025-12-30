package com.cuttypaws.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class PostRequestDto {

    private String caption;
    private List<MultipartFile> images;
}
