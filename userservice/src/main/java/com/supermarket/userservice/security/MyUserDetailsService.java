package com.supermarket.userservice.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.supermarket.userservice.exception.ResourceNotFoundException;
import com.supermarket.userservice.model.User;
import com.supermarket.userservice.model.UserPrincipal;
import com.supermarket.userservice.repository.UserRepository;


@Service
public class MyUserDetailsService implements UserDetailsService {
	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// TODO Auto-generated method stub
		User user = userRepository.findByEmail(username).orElseThrow(()->new UsernameNotFoundException("User not found with user email: " + username));

		if (user == null) {
			System.out.println("User not found");
			throw new ResourceNotFoundException("user not found");
		}
		System.out.println("user found successfully!");
		System.out.println(user.getRole().toString());
		return new UserPrincipal(user);
	}

}