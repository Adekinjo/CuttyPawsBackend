package com.cuttypaws.service.interf;


import com.cuttypaws.dto.PetRequestDto;
import com.cuttypaws.response.PetResponse;

public interface PetService {

    PetResponse createPet(Long userId, PetRequestDto request);

    PetResponse updatePet(Long petId, PetRequestDto request);

    PetResponse deletePet(Long petId);

    PetResponse getPetById(Long petId);

    PetResponse getMyPets(Long userId);

    PetResponse getAllPets();
}
