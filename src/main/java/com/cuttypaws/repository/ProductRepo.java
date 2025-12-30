
package com.cuttypaws.repository;

import com.cuttypaws.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepo extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.images " +
            "LEFT JOIN FETCH p.sizes " +
            "LEFT JOIN FETCH p.colors " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.subCategory " +
            "WHERE p.id = :id")
    Optional<Product> findByIdWithAssociations(@Param("id") Long id);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByNameContainingOrDescriptionContaining(String name, String description);
    List<Product> findByNameContaining(String name);

    @Query("SELECT p FROM Product p WHERE " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId)")
    List<Product> findByNameAndCategoryId(
            @Param("name") String name,
            @Param("categoryId") Long categoryId
    );

    @Query("SELECT p FROM Product p JOIN p.category c WHERE " +
            "(COALESCE(:name, '') = '' OR p.name LIKE %:name% OR p.description LIKE %:name% OR c.name LIKE %:name%) AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:minPrice IS NULL OR p.newPrice >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.newPrice <= :maxPrice)")
    List<Product> searchProducts(
            @Param("name") String name,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Product> findBySearchTerm(@Param("searchTerm") String searchTerm);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(p.category.name) LIKE LOWER(CONCAT('%', :categoryName, '%'))")
    List<Product> findByNameContainingIgnoreCaseOrCategoryNameContainingIgnoreCase(@Param("name") String name, @Param("categoryName") String categoryName);


    @Query("SELECT p FROM Product p WHERE p.subCategory.id = :subCategoryId")
    List<Product> findBySubCategoryId(@Param("subCategoryId") Long subCategoryId);

    // ProductRepo.java
    @Query("SELECT p FROM Product p WHERE p.lastViewedDate >= :cutoff")
    Page<Product> findTrendingProducts(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    @Query("SELECT p FROM Product p ORDER BY p.likes DESC")
    List<Product> findAllWithLikes();

    @Query("SELECT p FROM Product p WHERE p.user.id = :userId")
    List<Product> findByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"images"})
    List<Product> findTop4ByOrderByLikesDesc();

    @EntityGraph(attributePaths = {"images"})
    List<Product> findTop4ByOrderByViewCountDesc();

}
