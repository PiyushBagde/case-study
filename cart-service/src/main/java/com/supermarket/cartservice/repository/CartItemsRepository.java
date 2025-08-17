package com.supermarket.cartservice.repository;

import com.supermarket.cartservice.model.CartItems;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemsRepository extends JpaRepository<CartItems, Integer> {
    List<CartItems> findByCart_UserId(int userId); // Get all cart items for a user

    @Transactional
    void deleteByCart_CartId(int cartId);

    Optional<CartItems> findByCartCartIdAndProdName(int cartCartId, String prodName);
}
