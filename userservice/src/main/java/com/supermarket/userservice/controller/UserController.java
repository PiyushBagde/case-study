package com.supermarket.userservice.controller;

import com.supermarket.userservice.dto.UserResponse;
import com.supermarket.userservice.model.Role;
import com.supermarket.userservice.model.User;
import com.supermarket.userservice.service.UserServiceImpl;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/user")
@Validated // Enables validation for path variables and request parameters in this controller
public class UserController {
    @Autowired
    private UserServiceImpl service;

    // Updates the password for the authenticated user (Biller/Customer).
    @PutMapping("/biller-customer/updatePassword")
    public String updatePassword(
            @RequestHeader("X-UserId") int userId, // User ID extracted from header by gateway
            @RequestParam @NotBlank(message = "New password cannot be blank")
            @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
            String newPassword) {
        return service.updateUserPassword(userId, newPassword);
    }

    // Finds a specific user by email (for Biller/Admin lookup).
    @GetMapping("/biller/findUserByEmail")
    public UserResponse findUserByEmail(
            @RequestParam @NotBlank(message="Email cannot be blank") @Email(message="Invalid email format") String email // Validate email parameter
    ) {
        return service.findUserByEmailForBiller(email);
    }

    // Retrieves a list of all users (Admin only).
    @GetMapping("/admin/getAllUsers")
    public List<UserResponse> getAllUsers() {
        return service.getAllUser();
    }

    // Deletes a user by the specified user ID (Admin only).
    @DeleteMapping("/admin/deleteUser/{userId}")
    public String deleteUser(
            @PathVariable @Min(value = 1, message = "User ID must be a positive number") int userId // Validate path variable
    ) {
        service.deleteUserById(userId);
        return "user deleted successfully";
    }

    // Updates the role of a specific user (Admin only).
    @PutMapping("/admin/updateRole/{id}")
    public UserResponse updateUserRole(
            @PathVariable @Min(value = 1, message = "User ID must be a positive number") int id, // Validate path variable
            @RequestParam @NotNull(message = "New role cannot be null") Role newRole // Validate request parameter
    ) {
        User user = service.updateUserRole(id, newRole);
        // Map updated User entity to UserResponse DTO before returning
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    // Retrieves a specific user by their ID (Admin only).
    @GetMapping("/admin/getUser/{userId}")
    public UserResponse getUser(
            @PathVariable @Min(value = 1, message = "User ID must be a positive number") int userId // Validate path variable
    ) {
        return service.getUserByUserId(userId);
    }
}