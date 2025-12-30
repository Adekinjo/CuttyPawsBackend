package com.cuttypaws.controller;

import com.cuttypaws.dto.PetRequestDto;
import com.cuttypaws.response.*;
import com.cuttypaws.security.CurrentUser;
import com.cuttypaws.service.interf.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @PostMapping("/create")
    public ResponseEntity<PetResponse> createPet(
            @CurrentUser Long userId,
            @ModelAttribute PetRequestDto request
    ) {
        return ResponseEntity.ok(petService.createPet(userId, request));
    }

    @PutMapping("/{petId}")
    public ResponseEntity<PetResponse> updatePet(
            @PathVariable Long petId,
            @ModelAttribute PetRequestDto request
    ) {
        return ResponseEntity.ok(petService.updatePet(petId, request));
    }

    @DeleteMapping("/{petId}")
    public ResponseEntity<PetResponse> deletePet(@PathVariable Long petId) {
        return ResponseEntity.ok(petService.deletePet(petId));
    }

    @GetMapping("/{petId}")
    public ResponseEntity<PetResponse> getPet(@PathVariable Long petId) {
        return ResponseEntity.ok(petService.getPetById(petId));
    }

    @GetMapping("/my-pets")
    public ResponseEntity<PetResponse> getMyPets(@CurrentUser Long userId) {
        return ResponseEntity.ok(petService.getMyPets(userId));
    }

    @GetMapping("/all")
    public ResponseEntity<PetResponse> getAllPets() {
        return ResponseEntity.ok(petService.getAllPets());
    }
}
