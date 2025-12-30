package com.cuttypaws.repository;

import com.cuttypaws.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface CategoryRepo extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.subCategories WHERE c.id = :categoryId")
    Optional<Category> findByIdWithSubCategories(@Param("categoryId") Long categoryId);

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Category> findByNameContainingIgnoreCase(@Param("name") String name);

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Category> searchCategories(@Param("query") String query);

}
