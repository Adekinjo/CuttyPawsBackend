package com.cuttypaws.service.impl;

import com.cuttypaws.dto.*;
import com.cuttypaws.entity.*;
import com.cuttypaws.mapper.PetMapper;
import com.cuttypaws.repository.*;
import com.cuttypaws.response.PetResponse;
import com.cuttypaws.service.AwsS3Service;
import com.cuttypaws.service.interf.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PetServiceImpl implements PetService {

    private final PetRepo petRepo;
    private final UserRepo userRepo;
    private final AwsS3Service awsS3Service;
    private final PetMapper mapper;

    @Override
    @Caching(evict = {
            @CacheEvict(value="petsAll", allEntries = true),
            @CacheEvict(value="petsByUser", key="#userId"),
            @CacheEvict(value="petById", allEntries = true) // optional; if update/delete uses petId then target it
    })
    public PetResponse createPet(UUID userId, PetRequestDto request) {

        log.info("📌 [CREATE PET] Starting pet creation for userId: {}", userId);
        log.info("📌 Pet Request: {}", request);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("❌ User not found with ID {}", userId);
                    return new RuntimeException("User not found");
                });

        log.info("✔️ User found: {}", user.getEmail());

        Pet pet = Pet.builder()
                .name(request.getName())
                .type(request.getType())
                .breed(request.getBreed())
                .age(request.getAge())
                .gender(request.getGender())
                .description(request.getDescription())
                .user(user)
                .build();

        log.info("📸 Uploading pet images...");

        if (request.getImages() == null || request.getImages().isEmpty()) {
            log.error("❌ No images uploaded for pet");
            throw new RuntimeException("At least one pet image is required");
        }

        List<PetImage> images = new ArrayList<>();

        try {
            for (var file : request.getImages()) {

                log.info("➡️ Uploading file: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());

                String url = awsS3Service.uploadMedia(file);

                log.info("✔️ Image uploaded to S3: {}", url);

                images.add(
                        PetImage.builder()
                                .imageUrl(url)
                                .pet(pet)
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("❌ FAILED uploading image to S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload image to S3");
        }

        pet.setImages(images);

        log.info("📌 Saving pet to database...");

        Pet saved = petRepo.save(pet);

        log.info("✔️ Pet saved with ID: {}", saved.getId());

        return PetResponse.builder()
                .status(200)
                .message("Pet created successfully")
                .pet(mapper.mapPetToDto(saved))
                .build();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value="petsAll", allEntries = true),
            @CacheEvict(value="petsByUser", key="#userId"),
            @CacheEvict(value="petById", allEntries = true) // optional; if update/delete uses petId then target it
    })
    public PetResponse updatePet(Long petId, PetRequestDto request) {

        log.info("📌 [UPDATE PET] Updating pet ID: {}", petId);

        Pet pet = petRepo.findById(petId)
                .orElseThrow(() -> {
                    log.error("❌ Pet not found with ID {}", petId);
                    return new RuntimeException("Pet not found");
                });

        log.info("✔️ Pet found: {}", pet.getName());

        pet.setName(request.getName());
        pet.setType(request.getType());
        pet.setBreed(request.getBreed());
        pet.setAge(request.getAge());
        pet.setGender(request.getGender());
        pet.setDescription(request.getDescription());

        // Replace images if uploaded
        if (request.getImages() != null && !request.getImages().isEmpty()) {

            log.info("📸 New images uploaded. Replacing old images...");

            pet.getImages().clear();

            List<PetImage> newImages = new ArrayList<>();
            for (var file : request.getImages()) {
                try {
                    log.info("➡️ Uploading new image: {}", file.getOriginalFilename());
                    String url = awsS3Service.uploadMedia(file);

                    newImages.add(
                            PetImage.builder()
                                    .imageUrl(url)
                                    .pet(pet)
                                    .build()
                    );

                    log.info("✔️ New image saved: {}", url);

                } catch (Exception e) {
                    log.error("❌ Error uploading new pet image: {}", e.getMessage(), e);
                    throw new RuntimeException("Error uploading pet image");
                }
            }

            pet.setImages(newImages);
        }

        Pet updated = petRepo.save(pet);

        log.info("✔️ Pet updated successfully: {}", updated.getId());

        return PetResponse.builder()
                .status(200)
                .message("Pet updated successfully")
                .pet(mapper.mapPetToDto(updated))
                .build();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value="petsAll", allEntries = true),
            @CacheEvict(value="petsByUser", key="#userId"),
            @CacheEvict(value="petById", allEntries = true) // optional; if update/delete uses petId then target it
    })
    public PetResponse deletePet(Long petId) {
        log.info("🗑️ Deleting pet with ID: {}", petId);

        petRepo.deleteById(petId);

        log.info("✔️ Pet deleted");

        return PetResponse.builder()
                .status(200)
                .message("Pet deleted successfully")
                .build();
    }

    @Override
    @Cacheable(value="petById", key="#petId", condition="@cacheToggleService.isEnabled()",
            unless="#result == null || #result.pet == null")
    public PetResponse getPetById(Long petId) {
        log.info("📌 Fetching pet with ID: {}", petId);

        Pet pet = petRepo.findById(petId)
                .orElseThrow(() -> {
                    log.error("❌ Pet not found with ID {}", petId);
                    return new RuntimeException("Pet not found");
                });

        log.info("✔️ Pet found: {}", pet.getName());

        return PetResponse.builder()
                .status(200)
                .pet(mapper.mapPetToDto(pet))
                .build();
    }

    @Override
    @Cacheable(value="petsByUser", key="#userId", condition="@cacheToggleService.isEnabled()",
            unless="#result == null || #result.petList == null")
    public PetResponse getMyPets(UUID userId) {
        log.info("📌 Fetching pets for userId: {}", userId);

        List<Pet> pets = petRepo.findByUserId(userId);

        log.info("✔️ Found {} pets", pets.size());

        List<PetDto> dtoList = pets.stream()
                .map(mapper::mapPetToDto)
                .collect(Collectors.toList());

        return PetResponse.builder()
                .status(200)
                .petList(dtoList)
                .build();
    }

    @Override
    @Cacheable(value="petsAll", condition="@cacheToggleService.isEnabled()",
            unless="#result == null || #result.petList == null || #result.petList.isEmpty()")
    public PetResponse getAllPets() {
        log.info("📌 Fetching ALL pets...");

        List<PetDto> pets = petRepo.findAll().stream()
                .map(mapper::mapPetToDto)
                .collect(Collectors.toList());

        log.info("✔️ Total pets found: {}", pets.size());

        return PetResponse.builder()
                .status(200)
                .petList(pets)
                .build();
    }
}
