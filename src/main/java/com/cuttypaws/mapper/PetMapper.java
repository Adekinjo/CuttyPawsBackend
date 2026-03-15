package com.cuttypaws.mapper;

import com.cuttypaws.dto.PetDto;
import com.cuttypaws.entity.Pet;
import com.cuttypaws.entity.PetImage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PetMapper {

    public PetDto mapPetToDto(Pet pet) {
        List<String> imageUrls = pet.getImages() == null
                ? List.of()
                : pet.getImages().stream()
                .map(PetImage::getImageUrl)
                .toList();

        return PetDto.builder()
                .id(pet.getId())
                .name(pet.getName())
                .type(pet.getType())
                .breed(pet.getBreed())
                .normalizedBreed(pet.getNormalizedBreed())
                .age(pet.getAge())
                .gender(pet.getGender())
                .description(pet.getDescription())
                .size(pet.getSize())
                .color(pet.getColor())
                .activityLevel(pet.getActivityLevel())
                .temperament(pet.getTemperament())
                .vaccinated(pet.getVaccinated())
                .neutered(pet.getNeutered())
                .specialNeeds(pet.getSpecialNeeds())
                .city(pet.getCity())
                .state(pet.getState())
                .country(pet.getCountry())
                .viewCount(pet.getViewCount())
                .favoriteCount(pet.getFavoriteCount())
                .likeCount(pet.getLikeCount())
                .commentCount(pet.getCommentCount())
                .coverImageUrl(
                        pet.getCoverImageUrl() != null
                                ? pet.getCoverImageUrl()
                                : (imageUrls.isEmpty() ? null : imageUrls.get(0))
                )
                .tags(pet.getTags() == null ? List.of() : pet.getTags())
                .ownerId(pet.getUser() != null ? pet.getUser().getId() : null)
                .ownerName(pet.getUser() != null ? pet.getUser().getName() : null)
                .imageUrls(imageUrls)
                .build();
    }
}