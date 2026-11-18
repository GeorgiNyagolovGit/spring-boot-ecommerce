package com.shop.ecommerce.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI marketplaceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Marketplace Listings API")
                        .description(
                                """
                                REST API for authentication, public listings,
                                seller-owned ads, listing conversations,
                                categories and orders.

                                ### Quick test flow
                                1. Run `POST /api/auth/login` with one of the
                                   demo accounts.
                                2. Copy only the returned `token` value.
                                3. Select **Authorize**, paste the token and
                                   confirm.
                                4. Publish an ad with `POST /api/listings` or
                                   message its seller through
                                   `POST /api/conversations`.

                                Demo customer: `customer@example.com` /
                                `password123`

                                Demo administrator: `admin@example.com` /
                                `password123`
                                """
                        )
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }
}
