package com.supermarket.userservice.dto;

import com.supermarket.userservice.model.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private int userId;

    @NotBlank(message = "User name cannot be blank")
    @Size(min = 2, max = 100, message = "User name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;
}
