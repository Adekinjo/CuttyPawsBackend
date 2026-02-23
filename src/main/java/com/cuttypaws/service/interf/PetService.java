package com.cuttypaws.service.interf;


import com.cuttypaws.dto.PetRequestDto;
import com.cuttypaws.response.PetResponse;

import java.util.UUID;

public interface PetService {

    PetResponse createPet(UUID userId, PetRequestDto request);

    PetResponse updatePet(Long petId, PetRequestDto request);

    PetResponse deletePet(Long petId);

    PetResponse getPetById(Long petId);

    PetResponse getMyPets(UUID userId);

    PetResponse getAllPets();
}
