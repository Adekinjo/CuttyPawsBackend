package com.cuttypaws.repository;

import com.cuttypaws.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubCategoryRepo extends JpaRepository<SubCategory, Long> {

    @Query("SELECT s FROM SubCategory s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<SubCategory> findByNameContainingIgnoreCase(@Param("name") String name);

    @Query("SELECT s FROM SubCategory s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<SubCategory> searchSubCategories(@Param("query") String query);
}