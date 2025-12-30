package com.cuttypaws.mapper;

import com.cuttypaws.dto.PetDto;
import com.cuttypaws.entity.Pet;
import com.cuttypaws.entity.PetImage;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class PetMapper {


    public PetDto mapPetToDto(Pet pet) {
        return PetDto.builder()
                .id(pet.getId())
                .name(pet.getName())
                .type(pet.getType())
                .breed(pet.getBreed())
                .age(pet.getAge())
                .gender(pet.getGender())
                .description(pet.getDescription())
                .ownerId(pet.getUser().getId())
                .ownerName(pet.getUser().getName())
                .imageUrls(
                        pet.getImages().stream()
                                .map(PetImage::getImageUrl)
                                .collect(Collectors.toList())
                )
                .build();
    }

}
