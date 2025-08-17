package com.supermarket.userservice.controller;

import com.supermarket.userservice.dto.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.supermarket.userservice.model.User;
import com.supermarket.userservice.service.UserServiceImpl;

@RestController
@RequestMapping("/user")
public class AuthController {
	@Autowired
	private UserServiceImpl service;

	// login
	@PostMapping("/login")
	public String login(@Valid @RequestBody LoginRequest loginRequest) {
		System.out.println("** login controller passed");
		return service.verify(loginRequest);
	}
	// register
	@PostMapping("/register")
	public User createUser(@Valid @RequestBody User user) {
		return service.register(user);
	}
}
