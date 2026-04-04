package com.cuttypaws.service.impl;

import com.cuttypaws.dto.PetDto;
import com.cuttypaws.dto.PetRequestDto;
import com.cuttypaws.entity.Pet;
import com.cuttypaws.entity.PetImage;
import com.cuttypaws.entity.User;
import com.cuttypaws.mapper.PetMapper;
import com.cuttypaws.repository.PetRepo;
import com.cuttypaws.repository.UserRepo;
import com.cuttypaws.response.PetResponse;
import com.cuttypaws.service.AwsS3Service;
import com.cuttypaws.service.interf.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PetServiceImpl implements PetService {

    private final PetRepo petRepo;
    private final UserRepo userRepo;
    private final AwsS3Service awsS3Service;
    private final PetMapper mapper;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "petsAll", allEntries = true),
            @CacheEvict(value = "petsByUser", key = "#userId"),
            @CacheEvict(value = "petById", allEntries = true)
    })
    public PetResponse createPet(UUID userId, PetRequestDto request) {
        try {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            validatePetRequest(request, true);

            Pet pet = Pet.builder()
                    .name(request.getName().trim())
                    .type(request.getType().trim())
                    .breed(request.getBreed())
                    .normalizedBreed(normalizeBreed(request.getBreed()))
                    .age(request.getAge())
                    .gender(request.getGender())
                    .description(request.getDescription())
                    .size(request.getSize())
                    .color(request.getColor())
                    .activityLevel(request.getActivityLevel())
                    .temperament(request.getTemperament())
                    .vaccinated(request.getVaccinated())
                    .neutered(request.getNeutered())
                    .specialNeeds(request.getSpecialNeeds())
                    .city(request.getCity())
                    .state(request.getState())
                    .country(request.getCountry())
                    .tags(request.getTags() != null ? new ArrayList<>(request.getTags()) : new ArrayList<>())
                    .user(user)
                    .images(new ArrayList<>())
                    .build();

            List<PetImage> images = uploadPetImages(request, pet);
            pet.setImages(images);

            if (!images.isEmpty()) {
                int coverIndex = resolveCoverIndex(request.getCoverImageIndex(), images.size());
                pet.setCoverImageUrl(images.get(coverIndex).getImageUrl());
            }

            Pet saved = petRepo.save(pet);

            return PetResponse.builder()
                    .status(200)
                    .message("Pet created successfully")
                    .timeStamp(LocalDateTime.now())
                    .pet(mapper.mapPetToDto(saved))
                    .build();

        } catch (Exception e) {
            log.error("Error creating pet: {}", e.getMessage(), e);
            return PetResponse.builder()
                    .status(500)
                    .message(e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "petsAll", allEntries = true),
            @CacheEvict(value = "petsByUser", allEntries = true),
            @CacheEvict(value = "petById", allEntries = true)
    })
    public PetResponse updatePet(UUID userId, Long petId, PetRequestDto request) {
        try {
            Pet pet = petRepo.findWithDetailsById(petId)
                    .orElseThrow(() -> new RuntimeException("Pet not found"));

            if (pet.getUser() == null || !pet.getUser().getId().equals(userId)) {
                return PetResponse.builder()
                        .status(403)
                        .message("You can only update your own pet")
                        .timeStamp(LocalDateTime.now())
                        .build();
            }

            validatePetRequest(request, false);

            pet.setName(request.getName() != null ? request.getName().trim() : pet.getName());
            pet.setType(request.getType() != null ? request.getType().trim() : pet.getType());
            pet.setBreed(request.getBreed());
            pet.setNormalizedBreed(normalizeBreed(request.getBreed()));
            pet.setAge(request.getAge());
            pet.setGender(request.getGender());
            pet.setDescription(request.getDescription());
            pet.setSize(request.getSize());
            pet.setColor(request.getColor());
            pet.setActivityLevel(request.getActivityLevel());
            pet.setTemperament(request.getTemperament());
            pet.setVaccinated(request.getVaccinated());
            pet.setNeutered(request.getNeutered());
            pet.setSpecialNeeds(request.getSpecialNeeds());
            pet.setCity(request.getCity());
            pet.setState(request.getState());
            pet.setCountry(request.getCountry());
            pet.setTags(request.getTags() != null ? new ArrayList<>(request.getTags()) : new ArrayList<>());

            if (request.getImages() != null && !request.getImages().isEmpty()) {
                pet.getImages().clear();

                List<PetImage> newImages = uploadPetImages(request, pet);
                pet.setImages(newImages);

                if (!newImages.isEmpty()) {
                    int coverIndex = resolveCoverIndex(request.getCoverImageIndex(), newImages.size());
                    pet.setCoverImageUrl(newImages.get(coverIndex).getImageUrl());
                }
            }

            Pet updated = petRepo.save(pet);

            return PetResponse.builder()
                    .status(200)
                    .message("Pet updated successfully")
                    .timeStamp(LocalDateTime.now())
                    .pet(mapper.mapPetToDto(updated))
                    .build();

        } catch (Exception e) {
            log.error("Error updating pet: {}", e.getMessage(), e);
            return PetResponse.builder()
                    .status(500)
                    .message(e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "petsAll", allEntries = true),
            @CacheEvict(value = "petsByUser", allEntries = true),
            @CacheEvict(value = "petById", allEntries = true)
    })
    public PetResponse deletePet(UUID userId, Long petId) {
        try {
            Pet pet = petRepo.findById(petId)
                    .orElseThrow(() -> new RuntimeException("Pet not found"));

            if (pet.getUser() == null || !pet.getUser().getId().equals(userId)) {
                return PetResponse.builder()
                        .status(403)
                        .message("You can only delete your own pet")
                        .timeStamp(LocalDateTime.now())
                        .build();
            }

            petRepo.delete(pet);

            return PetResponse.builder()
                    .status(200)
                    .message("Pet deleted successfully")
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error deleting pet: {}", e.getMessage(), e);
            return PetResponse.builder()
                    .status(500)
                    .message(e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Cacheable(
            value = "petById",
            key = "#petId",
            condition = "@cacheToggleService.isEnabled()",
            unless = "#result == null || #result.pet == null"
    )
    @Transactional(readOnly = true)
    public PetResponse getPetById(Long petId) {
        try {
            Pet pet = petRepo.findWithDetailsById(petId)
                    .orElseThrow(() -> new RuntimeException("Pet not found"));

            return PetResponse.builder()
                    .status(200)
                    .timeStamp(LocalDateTime.now())
                    .pet(mapper.mapPetToDto(pet))
                    .build();

        } catch (Exception e) {
            log.error("Error fetching pet {}: {}", petId, e.getMessage(), e);
            return PetResponse.builder()
                    .status(500)
                    .message(e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Cacheable(
            value = "petsByUser",
            condition = "@cacheToggleService.isEnabled()"
    )
    @Transactional(readOnly = true)
    public PetResponse getMyPets(UUID userId) {
        try {
            List<PetDto> petDtos = petRepo.findByUserId(userId).stream()
                    .map(mapper::mapPetToDto)
                    .toList();

            return PetResponse.builder()
                    .status(200)
                    .timeStamp(LocalDateTime.now())
                    .petList(petDtos)
                    .build();

        } catch (Exception e) {
            log.error("Error fetching pets for user {}: {}", userId, e.getMessage(), e);
            return PetResponse.builder()
                    .status(500)
                    .message(e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Cacheable(
            value = "petsAll",
            condition = "@cacheToggleService.isEnabled()",
            unless = "#result == null || #result.petList == null || #result.petList.isEmpty()"
    )
    @Transactional(readOnly = true)
    public PetResponse getAllPets() {
        try {
            List<PetDto> petDtos = petRepo.findAll().stream()
                    .map(mapper::mapPetToDto)
                    .toList();

            return PetResponse.builder()
                    .status(200)
                    .timeStamp(LocalDateTime.now())
                    .petList(petDtos)
                    .build();

        } catch (Exception e) {
            log.error("Error fetching all pets: {}", e.getMessage(), e);
            return PetResponse.builder()
                    .status(500)
                    .message(e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    private void validatePetRequest(PetRequestDto request, boolean requireImages) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Pet name is required");
        }

        if (request.getType() == null || request.getType().trim().isEmpty()) {
            throw new RuntimeException("Pet type is required");
        }

        if (request.getAge() != null && request.getAge() < 0) {
            throw new RuntimeException("Pet age cannot be negative");
        }

        if (requireImages && (request.getImages() == null || request.getImages().isEmpty())) {
            throw new RuntimeException("At least one pet image is required");
        }
    }

    private List<PetImage> uploadPetImages(PetRequestDto request, Pet pet) {
        List<PetImage> images = new ArrayList<>();

        if (request.getImages() == null || request.getImages().isEmpty()) {
            return images;
        }

        for (var file : request.getImages()) {
            String url = awsS3Service.uploadMedia(file);
            images.add(
                    PetImage.builder()
                            .imageUrl(url)
                            .pet(pet)
                            .build()
            );
        }

        return images;
    }

    private int resolveCoverIndex(Integer coverImageIndex, int size) {
        if (coverImageIndex == null || coverImageIndex < 0 || coverImageIndex >= size) {
            return 0;
        }
        return coverImageIndex;
    }

    private String normalizeBreed(String breed) {
        if (breed == null || breed.isBlank()) {
            return null;
        }
        return breed.trim().toLowerCase();
    }
}