package com.cuttypaws.service.interf;

import com.cuttypaws.dto.PetRequestDto;
import com.cuttypaws.response.PetResponse;

import java.util.UUID;

public interface PetService {

    PetResponse createPet(UUID userId, PetRequestDto request);

    PetResponse updatePet(UUID userId, Long petId, PetRequestDto request);

    PetResponse deletePet(UUID userId, Long petId);

    PetResponse getPetById(Long petId);

    PetResponse getMyPets(UUID userId);

    PetResponse getAllPets();
}