package com.shop.ecommerce.product;

import com.shop.ecommerce.auth.entity.User;
import com.shop.ecommerce.auth.repository.UserRepository;
import com.shop.ecommerce.category.entity.Category;
import com.shop.ecommerce.category.repository.CategoryRepository;
import com.shop.ecommerce.product.dto.ListingRequest;
import com.shop.ecommerce.product.dto.ProductDto;
import com.shop.ecommerce.product.entity.Product;
import com.shop.ecommerce.product.entity.ProductCondition;
import com.shop.ecommerce.product.repository.ProductRepository;
import com.shop.ecommerce.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceMarketplaceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    private ProductService productService;
    private User seller;
    private Category category;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository,
                categoryRepository,
                userRepository
        );

        seller = new User();
        seller.setId(7L);
        seller.setFirstName("Ivan");
        seller.setLastName("Ivanov");
        seller.setEmail("seller@example.com");

        category = new Category();
        category.setId(3L);
        category.setName("Electronics");
    }

    @Test
    void createListingAssignsAuthenticatedUserAsOwner() {
        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(seller));
        when(categoryRepository.findByIdAndActiveTrue(3L))
                .thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    product.setId(11L);
                    return product;
                });

        ProductDto result = productService.createListing(
                "seller@example.com",
                request()
        );

        assertEquals(11L, result.getId());
        assertEquals(7L, result.getSellerId());
        assertEquals("Ivan Ivanov", result.getSellerName());
        assertEquals("Sofia", result.getLocation());
        assertEquals(ProductCondition.GOOD, result.getCondition());
    }

    @Test
    void ownerCanDeactivateOwnListing() {
        Product product = listingOwnedBy(seller);
        when(productRepository.findById(11L))
                .thenReturn(Optional.of(product));

        productService.deactivateListing("seller@example.com", 11L);

        assertFalse(product.isActive());
        assertSame(seller, product.getOwner());
    }

    @Test
    void anotherUserCannotChangeListing() {
        when(productRepository.findById(11L))
                .thenReturn(Optional.of(listingOwnedBy(seller)));

        assertThrows(
                AccessDeniedException.class,
                () -> productService.deactivateListing(
                        "other@example.com",
                        11L
                )
        );
    }

    private Product listingOwnedBy(User owner) {
        Product product = new Product();
        product.setId(11L);
        product.setOwner(owner);
        product.setActive(true);
        product.setCategory(category);
        return product;
    }

    private ListingRequest request() {
        return new ListingRequest(
                "Used phone",
                "Very good condition",
                new BigDecimal("499.00"),
                1,
                null,
                3L,
                "Sofia",
                ProductCondition.GOOD,
                "+359 888 123 456"
        );
    }
}
