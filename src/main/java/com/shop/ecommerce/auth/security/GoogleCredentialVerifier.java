package com.shop.ecommerce.auth.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.shop.ecommerce.common.exception.ExternalAuthenticationException;
import com.shop.ecommerce.common.exception.GoogleAuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Component
public class GoogleCredentialVerifier implements GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleCredentialVerifier(
            @Value("${google.auth.client-id:}") String clientId
    ) {
        if (clientId == null || clientId.isBlank()) {
            this.verifier = null;
            return;
        }

        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(List.of(clientId.trim()))
                .build();
    }

    @Override
    public GoogleIdentity verify(String credential) {
        if (verifier == null) {
            throw new ExternalAuthenticationException(
                    "Google authentication is not configured"
            );
        }

        try {
            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                throw new GoogleAuthenticationException(
                        "Invalid Google credential"
                );
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String subject = payload.getSubject();
            String email = payload.getEmail();
            boolean emailVerified = Boolean.TRUE.equals(
                    payload.getEmailVerified()
            );

            if (subject == null || subject.isBlank()
                    || email == null || email.isBlank()
                    || !emailVerified) {
                throw new GoogleAuthenticationException(
                        "Google account email is not verified"
                );
            }

            return new GoogleIdentity(
                    subject,
                    email,
                    true,
                    stringClaim(payload, "given_name"),
                    stringClaim(payload, "family_name"),
                    payload.getHostedDomain()
            );
        } catch (GeneralSecurityException | IOException exception) {
            throw new ExternalAuthenticationException(
                    "Google authentication is temporarily unavailable",
                    exception
            );
        }
    }

    private String stringClaim(
            GoogleIdToken.Payload payload,
            String name
    ) {
        Object value = payload.get(name);
        return value instanceof String string ? string : null;
    }
}
