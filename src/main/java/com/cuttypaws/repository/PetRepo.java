package com.cuttypaws.repository;

import com.cuttypaws.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PetRepo extends JpaRepository<Pet, Long> {

    List<Pet> findByUserId(UUID userId);
}
