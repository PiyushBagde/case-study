package com.supermarket.userservice.service;

import com.supermarket.userservice.dto.LoginRequest;
import com.supermarket.userservice.dto.UserResponse;
import com.supermarket.userservice.model.Role;
import com.supermarket.userservice.model.User;

import java.util.List;

public interface UserService {
    User register(User user);
    List<UserResponse> getAllUser();
    String updateUserPassword(int userId, String newPassword);
    void deleteUserById(int userId);
    User updateUserRole(int userId, Role newRole);
    UserResponse getUserByUserId(int userId);
    String verify(LoginRequest user);
    UserResponse findUserByEmailForBiller(String email);
}
