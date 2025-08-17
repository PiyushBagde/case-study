package com.supermarket.cartservice.service;

import java.util.List;

import com.supermarket.cartservice.model.Cart;
import com.supermarket.cartservice.model.CartItems;

public interface CartService {
	double calculateNewTotal(double currentTotal, double itemPriceChange, boolean increase);
	void addToCart(int userId, String prodName, int quantity);
	Cart getCartByUserId(int userId);
	void increaseQuantity(int userId, String prodName);
	void decreaseQuantity(int userId, String prodName);
	void removeItemFromCartInternal(Cart cart, CartItems item);
	void removeItemFromCart(int userId, String prodName);
	void clearCart(int userId);
	void clearCartContentsOnly(int userId);
	void deleteCart(int cartId);
	Cart getMyCart(int userId);
	List<CartItems> getCartItemsByUserId(int userId);
	
	
	
	 
	
	
}
