package com.cuttypaws.repository;

import com.cuttypaws.entity.Pet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetRepo extends JpaRepository<Pet, Long> {

    @EntityGraph(attributePaths = {"user", "images"})
    List<Pet> findByUserId(UUID userId);

    @Override
    @EntityGraph(attributePaths = {"user", "images"})
    List<Pet> findAll();

    @EntityGraph(attributePaths = {"user", "images"})
    Optional<Pet> findWithDetailsById(Long id);
}