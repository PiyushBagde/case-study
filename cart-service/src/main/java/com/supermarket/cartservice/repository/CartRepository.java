package com.supermarket.cartservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.supermarket.cartservice.model.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
	Optional<Cart> findByUserId(int userId);
}
