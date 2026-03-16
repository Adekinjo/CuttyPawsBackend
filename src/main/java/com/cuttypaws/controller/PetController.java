package com.cuttypaws.controller;

import com.cuttypaws.dto.PetRequestDto;
import com.cuttypaws.response.*;
import com.cuttypaws.security.CurrentUser;
import com.cuttypaws.service.interf.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @PostMapping("/create")
    public ResponseEntity<PetResponse> createPet(
            @CurrentUser UUID userId,
            @ModelAttribute PetRequestDto request
    ) {
        return ResponseEntity.ok(petService.createPet(userId, request));
    }

    @PutMapping("/{petId}")
    public ResponseEntity<PetResponse> updatePet(
            @CurrentUser UUID userId,
            @PathVariable Long petId,
            @ModelAttribute PetRequestDto request
    ) {
        return ResponseEntity.ok(petService.updatePet(userId, petId, request));
    }

    @DeleteMapping("/{petId}")
    public ResponseEntity<PetResponse> deletePet(@CurrentUser UUID userId, @PathVariable Long petId) {
        return ResponseEntity.ok(petService.deletePet(userId, petId));
    }

    @GetMapping("/{petId}")
    public ResponseEntity<PetResponse> getPet(@PathVariable Long petId) {
        return ResponseEntity.ok(petService.getPetById(petId));
    }

//    @GetMapping("/my-pets")
//    public ResponseEntity<PetResponse> getMyPets(@CurrentUser UUID userId) {
//        return ResponseEntity.ok(petService.getMyPets(userId));
//    }
    @GetMapping("/my-pets")
    public ResponseEntity<PetResponse> getMyPets(@CurrentUser UUID userId) {
        System.out.println("DEBUG /pet/my-pets userId = " + userId);
        PetResponse response = petService.getMyPets(userId);
        System.out.println("DEBUG /pet/my-pets service response = " + response);
        return ResponseEntity.ok(response);
    }

//    @GetMapping("/all")
//    public ResponseEntity<PetResponse> getAllPets() {
//        return ResponseEntity.ok(petService.getAllPets());
//    }
    @GetMapping("/all")
    public ResponseEntity<PetResponse> getAllPets() {
        System.out.println("DEBUG /pet/all called");
        PetResponse response = petService.getAllPets();
        System.out.println("DEBUG /pet/all response status = " + response.getStatus());
        return ResponseEntity.ok(response);
    }
}
