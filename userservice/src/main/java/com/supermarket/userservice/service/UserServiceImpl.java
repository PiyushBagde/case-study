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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService{

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthenticationManager authManager;
    @Autowired
    private JWTService jwtService;

    // Registers a new user with encoded password and default CUSTOMER role.
    @Override
    public User register(User user) {
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            throw new UserAlreadyExistsException("Registration failed: Email already in use: " + existingUser.get().getEmail());
        }

        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole(Role.CUSTOMER);
        try {
            return userRepository.save(user);
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to register user with email: " + user.getEmail(), e);
        } catch (Exception e) {
            throw new OperationFailedException("An unexpected error occurred during registration for email: " + user.getEmail(), e);
        }
    }

    // Retrieves all users from the database and maps them to UserResponse DTOs.
    @Override
    public List<UserResponse> getAllUser() {
        List<User> usersList = userRepository.findAll();

        if (usersList.isEmpty()) {
            throw new ResourceNotFoundException("No users found in the database.");
        }

        return usersList.stream()
                .map(user -> new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole()
                )).collect(Collectors.toList());
    }

    // Updates the password for a given user ID after validation.
    @Override
    public String updateUserPassword(int userId, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        if (newPassword.isBlank()) {
            throw new IllegalArgumentException("New password cannot be blank.");
        }
        if (encoder.matches(newPassword, user.getPassword())) {
            return "Password remain unchanged.";
        }
        try {
            user.setPassword(encoder.encode(newPassword));
            userRepository.save(user);
            return "Password updated successfully.";
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to update password for user ID: " + userId, e);
        } catch (Exception e) {
            throw new OperationFailedException("An unexpected error occurred during password update for user ID: " + userId, e);
        }
    }

    // Deletes a user by their ID after checking for existence.
    @Override
    public void deleteUserById(int userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Cannot delete. User not found with ID: " + userId);
        }
        try {
            userRepository.deleteById(userId);
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to delete user with ID: " + userId, e);
        } catch (Exception e) {
            throw new OperationFailedException("An unexpected error occurred during deletion for user ID: " + userId, e);
        }
    }

    // Updates the role for a given user ID.
    @Override
    public User updateUserRole(int userId, Role newRole) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId + " for role update."));
        user.setRole(newRole);
        try {
            return userRepository.save(user);
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to update role for user ID: " + userId, e);
        } catch (Exception e) {
            throw new OperationFailedException("An unexpected error occurred during role update for user ID: " + userId, e);
        }
    }

    // Retrieves a specific user by ID and maps to a UserResponse DTO.
    @Override
    public UserResponse getUserByUserId(int userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    // Verifies user credentials using AuthenticationManager and generates a JWT upon success.
    @Override
    public String verify(LoginRequest user) {
        Authentication authentication;
        try {
            authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));
        } catch (BadCredentialsException e) {
            throw new AuthenticationFailedException("Authentication failed: Invalid email or password.");
        } catch (Exception e) {
            throw new OperationFailedException("An unexpected error occurred during authentication.", e);
        }

        if (authentication.isAuthenticated()) {
            User authenticatedUser = userRepository.findByEmail(user.getEmail()).orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database: " + user.getEmail()));
            return jwtService.generateToken(authenticatedUser.getEmail(), authenticatedUser.getId());
        } else {
            throw new AuthenticationFailedException("Authentication failed for an unknown reason.");
        }
    }

    // Finds a user by email (typically for Biller/Admin lookup) and maps to UserResponse DTO.
    @Override
    public UserResponse findUserByEmailForBiller(@NotBlank @Email String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

}