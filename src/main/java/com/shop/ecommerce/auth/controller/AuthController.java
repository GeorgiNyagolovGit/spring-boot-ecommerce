package com.shop.ecommerce.auth.controller;

import com.shop.ecommerce.auth.dto.AuthResponse;
import com.shop.ecommerce.auth.dto.GoogleAuthConfigResponse;
import com.shop.ecommerce.auth.dto.GoogleLoginRequest;
import com.shop.ecommerce.auth.dto.LoginRequest;
import com.shop.ecommerce.auth.dto.RegisterRequest;
import com.shop.ecommerce.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register and log in users")
public class AuthController {

    private final AuthService authService;
    private final String googleClientId;

    public AuthController(
            AuthService authService,
            @Value("${google.auth.client-id:}") String googleClientId
    ) {
        this.authService = authService;
        this.googleClientId = googleClientId == null
                ? ""
                : googleClientId.trim();
    }

    @GetMapping("/google/config")
    @Operation(summary = "Get the public Google Sign-In configuration")
    public GoogleAuthConfigResponse googleAuthConfig() {
        return new GoogleAuthConfigResponse(
                googleClientId,
                !googleClientId.isBlank()
        );
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a customer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer registered"),
            @ApiResponse(responseCode = "400", description = "Invalid data or email already registered")
    })
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }

    @PostMapping("/google")
    @Operation(summary = "Sign in with a Google ID token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Google sign-in successful"),
            @ApiResponse(responseCode = "401", description = "Invalid Google credential"),
            @ApiResponse(responseCode = "503", description = "Google authentication unavailable")
    })
    public AuthResponse googleLogin(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        return authService.googleLogin(request);
    }
}
