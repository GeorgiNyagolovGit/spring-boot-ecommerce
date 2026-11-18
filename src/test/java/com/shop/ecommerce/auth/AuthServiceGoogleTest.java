package com.shop.ecommerce.auth;

import com.shop.ecommerce.auth.dto.AuthResponse;
import com.shop.ecommerce.auth.dto.GoogleLoginRequest;
import com.shop.ecommerce.auth.entity.Role;
import com.shop.ecommerce.auth.entity.User;
import com.shop.ecommerce.auth.repository.RoleRepository;
import com.shop.ecommerce.auth.repository.UserRepository;
import com.shop.ecommerce.auth.security.CustomUserDetailsService;
import com.shop.ecommerce.auth.security.GoogleTokenVerifier;
import com.shop.ecommerce.auth.security.JwtService;
import com.shop.ecommerce.auth.service.AuthService;
import com.shop.ecommerce.common.exception.GoogleAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceGoogleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    private AuthService authService;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                roleRepository,
                passwordEncoder,
                authenticationManager,
                userDetailsService,
                jwtService,
                googleTokenVerifier
        );

        customerRole = new Role();
        customerRole.setId(1L);
        customerRole.setName("ROLE_CUSTOMER");
    }

    @Test
    void googleLoginCreatesCustomerFromVerifiedIdentity() {
        GoogleTokenVerifier.GoogleIdentity identity = identity(
                "google-sub-1",
                "Ivan@Gmail.com",
                true,
                "Ivan",
                "Ivanov",
                null
        );
        when(googleTokenVerifier.verify("google-token"))
                .thenReturn(identity);
        when(userRepository.findByGoogleSubject("google-sub-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("ivan@gmail.com"))
                .thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_CUSTOMER"))
                .thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-random-password");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(7L);
                    return user;
                });
        when(jwtService.generateToken("ivan@gmail.com"))
                .thenReturn("application-jwt");

        AuthResponse response = authService.googleLogin(
                new GoogleLoginRequest("google-token")
        );

        assertEquals(7L, response.getId());
        assertEquals("ivan@gmail.com", response.getEmail());
        assertEquals("application-jwt", response.getToken());
        assertEquals(Set.of("ROLE_CUSTOMER"), response.getRoles());
        assertNotNull(response.getFirstName());
        verify(passwordEncoder).encode(anyString());
    }

    @Test
    void googleLoginUsesSubjectForReturningUser() {
        User user = googleUser();
        when(googleTokenVerifier.verify("google-token"))
                .thenReturn(identity(
                        "google-sub-1",
                        "ivan@gmail.com",
                        true,
                        "Ivan",
                        "Ivanov",
                        null
                ));
        when(userRepository.findByGoogleSubject("google-sub-1"))
                .thenReturn(Optional.of(user));
        when(jwtService.generateToken("ivan@gmail.com"))
                .thenReturn("application-jwt");

        AuthResponse response = authService.googleLogin(
                new GoogleLoginRequest("google-token")
        );

        assertEquals(7L, response.getId());
        assertEquals("application-jwt", response.getToken());
        verify(roleRepository, never()).findByName("ROLE_CUSTOMER");
    }

    @Test
    void googleLoginRejectsUnverifiedEmail() {
        when(googleTokenVerifier.verify("google-token"))
                .thenReturn(identity(
                        "google-sub-1",
                        "ivan@gmail.com",
                        false,
                        "Ivan",
                        "Ivanov",
                        null
                ));

        assertThrows(
                GoogleAuthenticationException.class,
                () -> authService.googleLogin(
                        new GoogleLoginRequest("google-token")
                )
        );
    }

    @Test
    void googleLoginDoesNotAutoLinkExternalEmailAccount() {
        User existing = googleUser();
        existing.setEmail("ivan@example.com");
        existing.setGoogleSubject(null);

        when(googleTokenVerifier.verify("google-token"))
                .thenReturn(identity(
                        "google-sub-1",
                        "ivan@example.com",
                        true,
                        "Ivan",
                        "Ivanov",
                        null
                ));
        when(userRepository.findByGoogleSubject("google-sub-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("ivan@example.com"))
                .thenReturn(Optional.of(existing));

        GoogleAuthenticationException exception = assertThrows(
                GoogleAuthenticationException.class,
                () -> authService.googleLogin(
                        new GoogleLoginRequest("google-token")
                )
        );

        assertEquals(
                "Google cannot be linked automatically to this account",
                exception.getMessage()
        );
    }

    private User googleUser() {
        User user = new User();
        user.setId(7L);
        user.setFirstName("Ivan");
        user.setLastName("Ivanov");
        user.setEmail("ivan@gmail.com");
        user.setGoogleSubject("google-sub-1");
        user.setPassword("encoded-password");
        user.setRoles(Set.of(customerRole));
        return user;
    }

    private GoogleTokenVerifier.GoogleIdentity identity(
            String subject,
            String email,
            boolean emailVerified,
            String givenName,
            String familyName,
            String hostedDomain
    ) {
        return new GoogleTokenVerifier.GoogleIdentity(
                subject,
                email,
                emailVerified,
                givenName,
                familyName,
                hostedDomain
        );
    }
}
