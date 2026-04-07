
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
import java.util.UUID;

public interface ProductRepo extends JpaRepository<Product, Long> {

    List<Product> findTop8ByNameContainingIgnoreCase(String name);

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

    @Query("SELECT p FROM Product p WHERE p.subCategory.id = :subCategoryId")
    List<Product> findBySubCategoryId(@Param("subCategoryId") Long subCategoryId);

    // ProductRepo.java
    @Query("SELECT p FROM Product p WHERE p.lastViewedDate >= :cutoff")
    Page<Product> findTrendingProducts(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    @Query("SELECT p FROM Product p ORDER BY p.likes DESC")
    List<Product> findAllWithLikes();

    @Query("SELECT p FROM Product p WHERE p.user.id = :userId")
    List<Product> findByUserId(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"images"})
    List<Product> findTop4ByOrderByLikesDesc();

    @EntityGraph(attributePaths = {"images"})
    List<Product> findTop4ByOrderByViewCountDesc();

    @Query("""
    SELECT DISTINCT p
    FROM Product p
    LEFT JOIN p.category c
    LEFT JOIN p.subCategory s
    WHERE
        LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%'))
        OR LOWER(p.description) LIKE LOWER(CONCAT('%', :term, '%'))
        OR LOWER(c.name) LIKE LOWER(CONCAT('%', :term, '%'))
        OR LOWER(s.name) LIKE LOWER(CONCAT('%', :term, '%'))
""")
    List<Product> searchBroadProductTerm(@Param("term") String term);

    @Query("""
    SELECT p
    FROM Product p
    WHERE p.stock IS NOT NULL
      AND p.stock > 0
    ORDER BY COALESCE(p.likes, 0) DESC, COALESCE(p.viewCount, 0) DESC, p.id DESC
""")
    List<Product> findFeedProductCandidates(org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {"category", "subCategory", "user"})
        @Query("""
    SELECT p
    FROM Product p
    WHERE p.id = :productId
    """)
    Optional<Product> findProductDetailsById(@Param("productId") Long productId);

    @Query("""
    SELECT DISTINCT p
    FROM Product p
    LEFT JOIN FETCH p.images
    WHERE p.id = :productId
    """)
    Optional<Product> findProductWithImages(@Param("productId") Long productId);

    @Query("""
    SELECT DISTINCT p
    FROM Product p
    LEFT JOIN FETCH p.colors
    WHERE p.id = :productId
    """)
    Optional<Product> findProductWithColors(@Param("productId") Long productId);

    @Query("""
    SELECT DISTINCT p
    FROM Product p
    LEFT JOIN FETCH p.sizes
    WHERE p.id = :productId
    """)
    Optional<Product> findProductWithSizes(@Param("productId") Long productId);

    @EntityGraph(attributePaths = {"images", "category", "subCategory", "user"})
    @Query("""
    SELECT p
    FROM Product p
    WHERE p.subCategory.id = :subCategoryId
    AND p.id <> :productId
    AND p.stock > 0
    ORDER BY COALESCE(p.likes,0) DESC, COALESCE(p.viewCount,0) DESC
    """)
    List<Product> findRelatedProductsBySubCategory(
            Long subCategoryId,
            Long productId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"images", "category", "subCategory", "user"})
    @Query("""
    SELECT p
    FROM Product p
    WHERE p.category.id = :categoryId
    AND p.id <> :productId
    AND p.stock > 0
    ORDER BY COALESCE(p.likes,0) DESC, COALESCE(p.viewCount,0) DESC
    """)
    List<Product> findOtherRelatedProductsByCategory(
            Long categoryId,
            Long productId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"images", "category", "subCategory", "user"})
    @Query("""
    SELECT p
    FROM Product p
    ORDER BY p.id DESC
    """)
    List<Product> findProductCards(Pageable pageable);

}
