package com.shop.ecommerce.auth.service;

import com.shop.ecommerce.auth.dto.AuthResponse;
import com.shop.ecommerce.auth.dto.GoogleLoginRequest;
import com.shop.ecommerce.auth.dto.LoginRequest;
import com.shop.ecommerce.auth.dto.RegisterRequest;
import com.shop.ecommerce.auth.entity.Role;
import com.shop.ecommerce.auth.entity.User;
import com.shop.ecommerce.auth.repository.RoleRepository;
import com.shop.ecommerce.auth.repository.UserRepository;
import com.shop.ecommerce.auth.security.CustomUserDetailsService;
import com.shop.ecommerce.auth.security.GoogleTokenVerifier;
import com.shop.ecommerce.auth.security.JwtService;
import com.shop.ecommerce.common.exception.GoogleAuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            JwtService jwtService,
            GoogleTokenVerifier googleTokenVerifier
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() ->
                        new IllegalStateException("Customer role not found"));

        User user = new User();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(customerRole));

        User savedUser = userRepository.save(user);

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(savedUser.getEmail());

        String token = jwtService.generateToken(userDetails.getUsername());

        return toAuthResponse(savedUser, token);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                email,
                                request.getPassword()
                        )
                );

        User user = userRepository.findByEmail(
                        authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException("User not found"));

        String token = jwtService.generateToken(user.getEmail());

        return toAuthResponse(user, token);
    }

    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleTokenVerifier.GoogleIdentity identity =
                googleTokenVerifier.verify(request.credential());

        if (!identity.emailVerified()) {
            throw new GoogleAuthenticationException(
                    "Google account email is not verified"
            );
        }

        String subject = requiredValue(
                identity.subject(),
                "Invalid Google account identifier"
        );
        String email = requiredValue(
                identity.email(),
                "Google account email is missing"
        ).toLowerCase(Locale.ROOT);

        if (email.indexOf('@') < 1 || email.length() > 150) {
            throw new GoogleAuthenticationException(
                    "Google account email is invalid"
            );
        }

        User user = userRepository.findByGoogleSubject(subject)
                .map(existing -> updateGoogleEmail(existing, email))
                .orElseGet(() -> linkOrCreateGoogleUser(
                        identity,
                        subject,
                        email
                ));

        String token = jwtService.generateToken(user.getEmail());
        return toAuthResponse(user, token);
    }

    private User linkOrCreateGoogleUser(
            GoogleTokenVerifier.GoogleIdentity identity,
            String subject,
            String email
    ) {
        return userRepository.findByEmail(email)
                .map(existing -> linkExistingUser(
                        existing,
                        identity,
                        subject
                ))
                .orElseGet(() -> createGoogleUser(
                        identity,
                        subject,
                        email
                ));
    }

    private User linkExistingUser(
            User user,
            GoogleTokenVerifier.GoogleIdentity identity,
            String subject
    ) {
        if (user.getGoogleSubject() != null
                && !user.getGoogleSubject().equals(subject)) {
            throw new GoogleAuthenticationException(
                    "Google account does not match the existing user"
            );
        }

        if (user.getGoogleSubject() == null
                && !isGoogleAuthoritativeForEmail(identity)) {
            throw new GoogleAuthenticationException(
                    "Google cannot be linked automatically to this account"
            );
        }

        user.setGoogleSubject(subject);
        return user;
    }

    private User createGoogleUser(
            GoogleTokenVerifier.GoogleIdentity identity,
            String subject,
            String email
    ) {
        Role customerRole = customerRole();

        User user = new User();
        user.setFirstName(normalizeName(
                identity.givenName(),
                email.substring(0, email.indexOf('@'))
        ));
        user.setLastName(normalizeName(identity.familyName(), "Google"));
        user.setEmail(email);
        user.setGoogleSubject(subject);
        user.setPassword(passwordEncoder.encode(
                UUID.randomUUID() + "-" + UUID.randomUUID()
        ));
        user.setRoles(Set.of(customerRole));

        return userRepository.save(user);
    }

    private User updateGoogleEmail(User user, String email) {
        if (user.getEmail().equalsIgnoreCase(email)) {
            return user;
        }

        User emailOwner = userRepository.findByEmail(email).orElse(null);
        if (emailOwner != null && !emailOwner.getId().equals(user.getId())) {
            throw new GoogleAuthenticationException(
                    "Google email is already used by another account"
            );
        }

        user.setEmail(email);
        return user;
    }

    private boolean isGoogleAuthoritativeForEmail(
            GoogleTokenVerifier.GoogleIdentity identity
    ) {
        String email = identity.email().toLowerCase(Locale.ROOT);
        return email.endsWith("@gmail.com")
                || (identity.hostedDomain() != null
                && !identity.hostedDomain().isBlank());
    }

    private String requiredValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new GoogleAuthenticationException(message);
        }
        return value.trim();
    }

    private String normalizeName(String value, String fallback) {
        String name = value == null || value.isBlank()
                ? fallback
                : value.trim();
        return name.length() <= 100 ? name : name.substring(0, 100);
    }

    private Role customerRole() {
        return roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() ->
                        new IllegalStateException("Customer role not found"));
    }

    private AuthResponse toAuthResponse(User user, String token) {
        Set<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new AuthResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                token,
                roles
        );
    }
}
