package com.shop.ecommerce.product.controller;

import com.shop.ecommerce.product.dto.ProductDto;
import com.shop.ecommerce.product.dto.ListingRequest;
import com.shop.ecommerce.product.dto.ListingStatusRequest;
import com.shop.ecommerce.common.config.OpenApiConfig;
import com.shop.ecommerce.product.entity.ProductCondition;
import com.shop.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping({"/api/listings", "/api/products"})
@Tag(name = "Listings", description = "Public marketplace listings")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List all active marketplace listings")
    public List<ProductDto> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a marketplace listing by ID")
    public ProductDto getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "List marketplace ads in a category")
    public List<ProductDto> getProductsByCategory(
            @PathVariable Long categoryId
    ) {
        return productService.getProductsByCategory(categoryId);
    }

    @GetMapping("/search")
    @Operation(summary = "Search ads by title, description or location")
    public List<ProductDto> searchProducts(
            @RequestParam String keyword
    ) {
        return productService.searchProducts(keyword);
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter active ads by category, location, condition and price")
    public List<ProductDto> filterListings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) ProductCondition condition,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        return productService.filterListings(
                keyword,
                categoryId,
                location,
                condition,
                minPrice,
                maxPrice
        );
    }

    @GetMapping("/seller/{sellerId}")
    @Operation(summary = "List all active ads from a seller")
    public List<ProductDto> getSellerListings(@PathVariable Long sellerId) {
        return productService.getSellerListings(sellerId);
    }

    @GetMapping("/mine")
    @Operation(summary = "List the current user's listings")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public List<ProductDto> getMyListings(Principal principal) {
        return productService.getMyListings(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publish a marketplace listing")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ProductDto createListing(
            Principal principal,
            @Valid @RequestBody ListingRequest request
    ) {
        return productService.createListing(principal.getName(), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update one of the current user's listings")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ProductDto updateListing(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody ListingRequest request
    ) {
        return productService.updateListing(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate one of the current user's listings")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public void deactivateListing(Principal principal, @PathVariable Long id) {
        productService.deactivateListing(principal.getName(), id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate one of the current user's listings")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ProductDto setListingStatus(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody ListingStatusRequest request
    ) {
        return productService.setListingActive(
                principal.getName(),
                id,
                request.active()
        );
    }
}
