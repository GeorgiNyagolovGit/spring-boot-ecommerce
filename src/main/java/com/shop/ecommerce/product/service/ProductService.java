package com.shop.ecommerce.product.service;

import com.shop.ecommerce.auth.entity.User;
import com.shop.ecommerce.auth.repository.UserRepository;
import com.shop.ecommerce.category.entity.Category;
import com.shop.ecommerce.category.repository.CategoryRepository;
import com.shop.ecommerce.common.exception.NotFoundException;
import com.shop.ecommerce.product.dto.ListingRequest;
import com.shop.ecommerce.product.dto.ProductDto;
import com.shop.ecommerce.product.entity.Product;
import com.shop.ecommerce.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import com.shop.ecommerce.product.entity.ProductCondition;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public List<ProductDto> getAllProducts() {
        return productRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        return toDto(product);
    }

    public List<ProductDto> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndActiveTrue(categoryId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<ProductDto> searchProducts(String keyword) {
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword == null) {
            return getAllProducts();
        }

        return productRepository.searchByKeyword(normalizedKeyword)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<ProductDto> filterListings(
            String keyword,
            Long categoryId,
            String location,
            ProductCondition condition,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        if (minPrice != null && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException(
                    "Minimum price cannot be greater than maximum price"
            );
        }

        return productRepository.filterListings(
                        trimToNull(keyword),
                        categoryId,
                        trimToNull(location),
                        condition,
                        minPrice,
                        maxPrice
                )
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<ProductDto> getSellerListings(Long sellerId) {
        if (!userRepository.existsById(sellerId)) {
            throw new NotFoundException("Seller not found");
        }

        return productRepository
                .findByOwnerIdAndActiveTrueOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<ProductDto> getMyListings(String email) {
        User owner = findUser(email);
        return productRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ProductDto createListing(String email, ListingRequest request) {
        User owner = findUser(email);
        Product product = new Product();
        product.setOwner(owner);
        product.setActive(true);
        applyListing(product, request);
        return toDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto updateListing(
            String email,
            Long id,
            ListingRequest request
    ) {
        Product product = findOwnedListing(email, id);
        applyListing(product, request);
        return toDto(product);
    }

    @Transactional
    public void deactivateListing(String email, Long id) {
        findOwnedListing(email, id).setActive(false);
    }

    @Transactional
    public ProductDto setListingActive(String email, Long id, boolean active) {
        Product product = findOwnedListing(email, id);
        product.setActive(active);
        return toDto(product);
    }

    private void applyListing(Product product, ListingRequest request) {
        Category category = categoryRepository
                .findByIdAndActiveTrue(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        product.setName(request.name().trim());
        product.setDescription(request.description().trim());
        product.setPrice(request.price());
        product.setStock(request.stock() == null ? 1 : request.stock());
        product.setImageUrl(trimToNull(request.imageUrl()));
        product.setCategory(category);
        product.setLocation(request.location().trim());
        product.setCondition(request.condition());
        product.setContactPhone(request.contactPhone().trim());
    }

    private Product findOwnedListing(String email, Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Listing not found"));

        if (product.getOwner() == null
                || !product.getOwner().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException(
                    "You can only manage your own listings"
            );
        }
        return product;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ProductDto toDto(Product product) {
        User owner = product.getOwner();
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getImageUrl(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getLocation(),
                product.getCondition(),
                product.getContactPhone(),
                owner == null ? null : owner.getId(),
                owner == null ? null : owner.getFirstName() + " "
                        + owner.getLastName(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
