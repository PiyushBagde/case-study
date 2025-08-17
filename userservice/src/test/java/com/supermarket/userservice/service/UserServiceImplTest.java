package com.supermarket.userservice.service;

import com.supermarket.userservice.dto.LoginRequest;
import com.supermarket.userservice.dto.UserResponse;
import com.supermarket.userservice.exception.AuthenticationFailedException;
import com.supermarket.userservice.exception.OperationFailedException;
import com.supermarket.userservice.exception.ResourceNotFoundException;
import com.supermarket.userservice.exception.UserAlreadyExistsException;
import com.supermarket.userservice.model.Role;
import com.supermarket.userservice.model.User;
import com.supermarket.userservice.repository.UserRepository;
import com.supermarket.userservice.security.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*; // JUnit 5 assertions
import static org.mockito.ArgumentMatchers.*; // Mockito matchers like any(), anyInt(), anyString()
import static org.mockito.Mockito.*; // Mockito static methods like when(), verify()

@ExtendWith(MockitoExtension.class) // Initialize Mockito
class UserServiceImplTest {

    @Mock // Create a mock instance of UserRepository
    private UserRepository userRepository;

    @Mock // Create a mock instance of AuthenticationManager
    private AuthenticationManager authManager;

    @Mock // Create a mock instance of JWTService
    private JWTService jwtService;

    @InjectMocks // Create an instance of UserService and inject the mocks into it
    private UserServiceImpl userServiceImpl;

    // Optional: If you need to compare encoded passwords, keep an encoder instance
    private final BCryptPasswordEncoder testEncoder = new BCryptPasswordEncoder();

    private User sampleUser;
    private LoginRequest sampleLoginRequest;

    @BeforeEach // Setup method runs before each test
    void setUp() {
        // Initialize common test objects
        sampleUser = new User(1, "Test User", "test@example.com", "password123", Role.CUSTOMER);
        sampleLoginRequest = new LoginRequest();
        sampleLoginRequest.setEmail("test@example.com");
        sampleLoginRequest.setPassword("password123");
    }

    // --- Tests for register() ---

    @Test
    @DisplayName("Register: Success when email does not exist")
    void register_WhenEmailDoesNotExist_ShouldSaveAndReturnUser() {
        // Arrange
        User newUser = new User(0, "New User", "new@example.com", "newPassword", null); // ID 0, role null initially
        User savedUser = new User(5, "New User", "new@example.com", "encodedPassword", Role.CUSTOMER); // Expected saved state

        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.empty());
        // Mock the save operation. Use ArgumentCaptor to check the encoded password and role if needed.
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);
            // Simulate saving by assigning an ID and returning
            savedUser.setPassword(userToSave.getPassword()); // Keep the encoded password from the service call
            return savedUser;
        });

        // Act
        User result = userServiceImpl.register(newUser);

        // Assert
        assertNotNull(result);
        assertEquals(savedUser.getId(), result.getId());
        assertEquals(savedUser.getEmail(), result.getEmail());
        assertEquals(Role.CUSTOMER, result.getRole()); // Check default role
        assertTrue(testEncoder.matches("newPassword", result.getPassword()), "Password should be encoded"); // Verify password encoding

        verify(userRepository).findByEmail("new@example.com"); // Verify findByEmail was called
        verify(userRepository).save(any(User.class)); // Verify save was called
    }

    @Test
    @DisplayName("Register: Throws UserAlreadyExistsException when email exists")
    void register_WhenEmailExists_ShouldThrowUserAlreadyExistsException() {
        // Arrange
        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));

        // Act & Assert
        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> {
            userServiceImpl.register(sampleUser);
        });

        assertTrue(exception.getMessage().contains(sampleUser.getEmail()));
        verify(userRepository).findByEmail(sampleUser.getEmail());
        verify(userRepository, never()).save(any(User.class)); // Ensure save is never called
    }

    @Test
    @DisplayName("Register: Throws OperationFailedException on database save error")
    void register_WhenDbSaveFails_ShouldThrowOperationFailedException() {
        // Arrange
        User newUser = new User(0, "Fail User", "fail@example.com", "password", null);
        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.empty());
        // Simulate DataAccessException during save
        when(userRepository.save(any(User.class))).thenThrow(new DataAccessException("DB connection failed") {}); // Anonymous subclass

        // Act & Assert
        OperationFailedException exception = assertThrows(OperationFailedException.class, () -> {
            userServiceImpl.register(newUser);
        });

        assertTrue(exception.getMessage().contains("Failed to register user"));
        verify(userRepository).findByEmail(newUser.getEmail());
        verify(userRepository).save(any(User.class));
    }

    // --- Tests for getAllUser() ---

    @Test
    @DisplayName("GetAllUser: Success returns list of UserResponses")
    void getAllUser_WhenUsersExist_ShouldReturnUserResponseList() {
        // Arrange
        User user1 = new User(1, "User One", "one@example.com", "pass1", Role.CUSTOMER);
        User user2 = new User(2, "User Two", "two@example.com", "pass2", Role.ADMIN);
        List<User> userList = List.of(user1, user2);
        when(userRepository.findAll()).thenReturn(userList);

        // Act
        List<UserResponse> result = userServiceImpl.getAllUser();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(user1.getId(), result.getFirst().getUserId());
        assertEquals(user1.getName(), result.getFirst().getName());
        assertEquals(user1.getEmail(), result.getFirst().getEmail());
        assertEquals(user1.getRole(), result.getFirst().getRole());
        assertEquals(user2.getId(), result.get(1).getUserId());

        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("GetAllUser: Throws ResourceNotFoundException when no users exist")
    void getAllUser_WhenNoUsers_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userServiceImpl.getAllUser();
        });
        verify(userRepository).findAll();
    }


    // --- Tests for updateUserPassword() ---

    @Test
    @DisplayName("UpdatePassword: Success updates password")
    void updateUserPassword_WhenUserExistsAndPasswordDiffers_ShouldUpdateAndReturnSuccess() {
        // Arrange
        int userId = sampleUser.getId();
        String oldEncodedPassword = testEncoder.encode("oldPassword");
        sampleUser.setPassword(oldEncodedPassword); // Set an existing encoded password
        String newPassword = "newPassword123";

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0)); // Return the saved user

        // Act
        String result = userServiceImpl.updateUserPassword(userId, newPassword);

        // Assert
        assertEquals("Password updated successfully.", result);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(testEncoder.matches(newPassword, userCaptor.getValue().getPassword()), "New password should be encoded");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("UpdatePassword: Returns unchanged message if password matches")
    void updateUserPassword_WhenPasswordMatches_ShouldReturnUnchangedMessage() {
        // Arrange
        int userId = sampleUser.getId();
        String currentPassword = "currentPassword";
        String currentEncodedPassword = testEncoder.encode(currentPassword);
        sampleUser.setPassword(currentEncodedPassword);

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        // Act
        String result = userServiceImpl.updateUserPassword(userId, currentPassword);

        // Assert
        assertEquals("Password remain unchanged.", result);
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class)); // Save should not be called
    }

    @Test
    @DisplayName("UpdatePassword: Throws IllegalArgumentException for blank password")
    void updateUserPassword_WhenNewPasswordIsBlank_ShouldThrowIllegalArgumentException() {
        // Arrange
        int userId = sampleUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser)); // Need user to exist for the check

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userServiceImpl.updateUserPassword(userId, "   "); // Blank password
        });
        assertEquals("New password cannot be blank.", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("UpdatePassword: Throws ResourceNotFoundException if user not found")
    void updateUserPassword_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        int userId = 999;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userServiceImpl.updateUserPassword(userId, "newPassword");
        });
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    // --- Tests for deleteUserById() ---

    @Test
    @DisplayName("DeleteUserById: Success deletes user when exists")
    void deleteUserById_WhenUserExists_ShouldCallDelete() {
        // Arrange
        int userId = sampleUser.getId();
        when(userRepository.existsById(userId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(userId); // Mock void method

        // Act
        assertDoesNotThrow(() -> userServiceImpl.deleteUserById(userId)); // Verify no exception is thrown

        // Assert
        verify(userRepository).existsById(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    @DisplayName("DeleteUserById: Throws ResourceNotFoundException when user not exists")
    void deleteUserById_WhenUserNotExists_ShouldThrowResourceNotFoundException() {
        // Arrange
        int userId = 999;
        when(userRepository.existsById(userId)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userServiceImpl.deleteUserById(userId);
        });

        verify(userRepository).existsById(userId);
        verify(userRepository, never()).deleteById(userId);
    }

    @Test
    @DisplayName("DeleteUserById: Throws OperationFailedException on delete error")
    void deleteUserById_WhenDeleteFails_ShouldThrowOperationFailedException() {
        // Arrange
        int userId = sampleUser.getId();
        when(userRepository.existsById(userId)).thenReturn(true);
        // Simulate exception during delete
        doThrow(new DataAccessException("DB delete error") {}).when(userRepository).deleteById(userId);

        // Act & Assert
        assertThrows(OperationFailedException.class, () -> {
            userServiceImpl.deleteUserById(userId);
        });

        verify(userRepository).existsById(userId);
        verify(userRepository).deleteById(userId);
    }

    // --- Tests for updateUserRole() ---

    @Test
    @DisplayName("UpdateUserRole: Success updates role")
    void updateUserRole_WhenUserExists_ShouldUpdateRoleAndReturnUser() {
        // Arrange
        int userId = sampleUser.getId();
        Role newRole = Role.ADMIN;
        User updatedUser = new User(userId, sampleUser.getName(), sampleUser.getEmail(), sampleUser.getPassword(), newRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser); // Return the expected updated user

        // Act
        User result = userServiceImpl.updateUserRole(userId, newRole);

        // Assert
        assertNotNull(result);
        assertEquals(newRole, result.getRole());
        assertEquals(userId, result.getId());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(newRole, userCaptor.getValue().getRole()); // Verify role was set before save
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("UpdateUserRole: Throws ResourceNotFoundException if user not found")
    void updateUserRole_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        int userId = 999;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userServiceImpl.updateUserRole(userId, Role.ADMIN);
        });
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    // --- Tests for getUserByUserId() ---
    @Test
    @DisplayName("GetUserById: Success returns UserResponse when found")
    void getUserByUserId_WhenUserFound_ShouldReturnUserResponse() {
        // Arrange
        int userId = sampleUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        // Act
        UserResponse result = userServiceImpl.getUserByUserId(userId);

        // Assert
        assertNotNull(result);
        assertEquals(sampleUser.getId(), result.getUserId());
        assertEquals(sampleUser.getName(), result.getName());
        assertEquals(sampleUser.getEmail(), result.getEmail());
        assertEquals(sampleUser.getRole(), result.getRole());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("GetUserById: Throws ResourceNotFoundException when not found")
    void getUserByUserId_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        int userId = 999;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userServiceImpl.getUserByUserId(userId);
        });
        verify(userRepository).findById(userId);
    }


    // --- Tests for verify() ---

    @Test
    @DisplayName("Verify: Success returns JWT token on valid credentials")
    void verify_WhenCredentialsValid_ShouldReturnToken() {
        // Arrange
        Authentication mockAuthentication = mock(Authentication.class); // Mock the Authentication object
        String expectedToken = "mock.jwt.token.123";

        // Mock authManager behavior
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);

        // Mock repository lookup after successful authentication
        when(userRepository.findByEmail(sampleLoginRequest.getEmail())).thenReturn(Optional.of(sampleUser));

        // Mock JWT service behavior
        when(jwtService.generateToken(sampleUser.getEmail(), sampleUser.getId())).thenReturn(expectedToken);

        // Act
        String resultToken = userServiceImpl.verify(sampleLoginRequest);

        // Assert
        assertNotNull(resultToken);
        assertEquals(expectedToken, resultToken);

        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail(sampleLoginRequest.getEmail());
        verify(jwtService).generateToken(sampleUser.getEmail(), sampleUser.getId());
    }

    @Test
    @DisplayName("Verify: Throws AuthenticationFailedException on BadCredentialsException")
    void verify_WhenAuthManagerThrowsBadCredentials_ShouldThrowAuthenticationFailedException() {
        // Arrange
        // Mock authManager to throw BadCredentialsException
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials: Invalid email or password."));

        // Act & Assert
        AuthenticationFailedException exception = assertThrows(AuthenticationFailedException.class, () -> {
            userServiceImpl.verify(sampleLoginRequest);
        });

        assertEquals("Authentication failed: Invalid email or password.", exception.getMessage());
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmail(anyString()); // Should not reach repo lookup
        verify(jwtService, never()).generateToken(anyString(), anyInt()); // Should not reach token generation
    }

    @Test
    @DisplayName("Verify: Throws OperationFailedException on other Authentication Exception")
    void verify_WhenAuthManagerThrowsOtherException_ShouldThrowOperationFailedException() {
        // Arrange
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Some other auth error")); // Simulate a non-BadCredentials error

        // Act & Assert
        assertThrows(OperationFailedException.class, () -> {
            userServiceImpl.verify(sampleLoginRequest);
        });
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtService, never()).generateToken(anyString(), anyInt());
    }

    @Test
    @DisplayName("Verify: Throws ResourceNotFoundException if user not found after successful auth")
    void verify_WhenUserNotFoundAfterAuth_ShouldThrowResourceNotFoundException() {
        // Arrange
        Authentication mockAuthentication = mock(Authentication.class);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        // Simulate user not found in DB after successful authentication (edge case)
        when(userRepository.findByEmail(sampleLoginRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userServiceImpl.verify(sampleLoginRequest);
        });

        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail(sampleLoginRequest.getEmail());
        verify(jwtService, never()).generateToken(anyString(), anyInt());
    }

    @Test
    @DisplayName("Verify: Throws AuthenticationFailedException if authentication not authenticated")
    void verify_WhenAuthenticationNotAuthenticated_ShouldThrowAuthenticationFailedException() {
        // Arrange
        Authentication mockAuthentication = mock(Authentication.class);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);
        // Simulate isAuthenticated() returning false
        when(mockAuthentication.isAuthenticated()).thenReturn(false);

        // Act & Assert
        assertThrows(AuthenticationFailedException.class, () -> {
            userServiceImpl.verify(sampleLoginRequest);
        });
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtService, never()).generateToken(anyString(), anyInt());
    }
}