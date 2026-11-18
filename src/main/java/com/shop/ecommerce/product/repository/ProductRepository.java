package com.shop.ecommerce.product.repository;

import com.shop.ecommerce.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.shop.ecommerce.product.entity.ProductCondition;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByCreatedAtDesc();

    Optional<Product> findByIdAndActiveTrue(Long id);

    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    List<Product> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<Product> findByOwnerIdAndActiveTrueOrderByCreatedAtDesc(Long ownerId);

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
            AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(COALESCE(p.location, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY p.createdAt DESC
            """)
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
            AND (:keyword IS NULL
              OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:categoryId IS NULL OR p.category.id = :categoryId)
            AND (:location IS NULL
              OR LOWER(COALESCE(p.location, '')) LIKE LOWER(CONCAT('%', :location, '%')))
            AND (:condition IS NULL OR p.condition = :condition)
            AND (:minPrice IS NULL OR p.price >= :minPrice)
            AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            ORDER BY p.createdAt DESC
            """)
    List<Product> filterListings(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("location") String location,
            @Param("condition") ProductCondition condition,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );
}
