package com.collab;

import com.collab.dto.*;
import com.collab.model.User;
import com.collab.repository.UserRepository;
import com.collab.security.JwtUtil;
import com.collab.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "JWT_SECRET=test-only-signing-material-not-for-deployment-123456")
@Transactional
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private UserRegistrationRequest registrationRequest;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
        registrationRequest = new UserRegistrationRequest(
            "testuser",
            "test@example.com",
            "password123",
            "Test User"
        );
    }

    @Test
    public void testUserRegistration_Success() {
        UserResponse response = userService.registerUser(registrationRequest);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        assertTrue(response.isActive());
    }

    @Test
    public void testUserRegistration_DuplicateUsername() {
        userService.registerUser(registrationRequest);

        UserRegistrationRequest duplicateRequest = new UserRegistrationRequest(
            "testuser",
            "another@example.com",
            "password123",
            "Another User"
        );

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(duplicateRequest);
        });

        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    public void testUserRegistration_DuplicateEmail() {
        userService.registerUser(registrationRequest);

        UserRegistrationRequest duplicateRequest = new UserRegistrationRequest(
            "anotheruser",
            "test@example.com",
            "password123",
            "Another User"
        );

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(duplicateRequest);
        });

        assertEquals("Email already exists", exception.getMessage());
    }

    @Test
    public void testUserAuthentication_Success() {
        userService.registerUser(registrationRequest);

        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        AuthResponse authResponse = userService.authenticateUser(loginRequest);

        assertNotNull(authResponse);
        assertNotNull(authResponse.getToken());
        assertEquals("testuser", authResponse.getUsername());
        assertEquals("Authentication successful", authResponse.getMessage());
    }

    @Test
    public void testUserAuthentication_InvalidUsername() {
        userService.registerUser(registrationRequest);

        LoginRequest loginRequest = new LoginRequest("wronguser", "password123");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.authenticateUser(loginRequest);
        });

        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    public void testUserAuthentication_InvalidPassword() {
        userService.registerUser(registrationRequest);

        LoginRequest loginRequest = new LoginRequest("testuser", "wrongpassword");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.authenticateUser(loginRequest);
        });

        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    public void testGetUserProfile_Success() {
        userService.registerUser(registrationRequest);

        UserResponse profile = userService.getProfile("testuser");

        assertNotNull(profile);
        assertEquals("testuser", profile.getUsername());
        assertEquals("test@example.com", profile.getEmail());
        assertEquals("Test User", profile.getFullName());
    }

    @Test
    public void testGetUserProfile_UserNotFound() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.getProfile("nonexistent");
        });

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    public void testUpdateUserProfile_Success() {
        userService.registerUser(registrationRequest);

        ProfileUpdateRequest updateRequest = new ProfileUpdateRequest(
            "Updated Name",
            "This is my bio",
            "http://example.com/pic.jpg"
        );

        UserResponse updatedProfile = userService.updateProfile("testuser", updateRequest);

        assertNotNull(updatedProfile);
        assertEquals("Updated Name", updatedProfile.getFullName());
        assertEquals("This is my bio", updatedProfile.getBio());
        assertEquals("http://example.com/pic.jpg", updatedProfile.getProfilePictureUrl());
    }

    @Test
    public void testGetUserById_Success() {
        UserResponse registered = userService.registerUser(registrationRequest);

        UserResponse retrieved = userService.getUserById(registered.getId());

        assertNotNull(retrieved);
        assertEquals(registered.getId(), retrieved.getId());
        assertEquals("testuser", retrieved.getUsername());
    }

    @Test
    public void testGetUserById_NotFound() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserById(999L);
        });

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    public void testPasswordEncryption() {
        userService.registerUser(registrationRequest);

        User user = userRepository.findByUsername("testuser").orElseThrow();
        
        assertNotEquals("password123", user.getPassword());
        assertTrue(passwordEncoder.matches("password123", user.getPassword()));
    }

    @Test
    public void testJwtTokenGeneration() {
        userService.registerUser(registrationRequest);

        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        AuthResponse authResponse = userService.authenticateUser(loginRequest);

        String token = authResponse.getToken();
        assertNotNull(token);

        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals("testuser", extractedUsername);

        assertTrue(jwtUtil.validateToken(token, "testuser"));
    }
}
