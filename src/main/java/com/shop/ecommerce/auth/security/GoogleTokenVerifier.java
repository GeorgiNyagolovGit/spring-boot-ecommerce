package com.shop.ecommerce.auth.security;

public interface GoogleTokenVerifier {

    GoogleIdentity verify(String credential);

    record GoogleIdentity(
            String subject,
            String email,
            boolean emailVerified,
            String givenName,
            String familyName,
            String hostedDomain
    ) {
    }
}
