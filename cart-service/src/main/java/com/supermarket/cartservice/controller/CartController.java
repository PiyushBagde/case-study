package com.supermarket.cartservice.controller;

import com.supermarket.cartservice.model.Cart;
import com.supermarket.cartservice.model.CartItems;
import com.supermarket.cartservice.service.CartServiceImpl;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@Validated
public class CartController {

    @Autowired
    private CartServiceImpl cartService;

    // routes for customer
    @PostMapping("/biller/addToCart")
    public String addToCart(
            @RequestParam @Min(value = 1, message = "User ID must be positive") int userId,
            @RequestParam @NotBlank(message = "Product name cannot be blank") String prodName,
            @RequestParam @Positive(message = "Quantity must be positive") int quantity) {
        // TODO check when user tries to add same product again
        System.out.println("Adding cart to user " + userId + " product " + prodName);
        cartService.addToCart(userId, prodName, quantity);
        return prodName + " added to cart of userId" + userId;
    }

    @DeleteMapping("/biller/removeItemFromCart")
    public String removeItemFromCart(
            @RequestParam @Min(value = 1, message = "User ID must be positive") int userId,
            @RequestParam @NotBlank(message = "Product name cannot be blank") String prodName) {
        cartService.removeItemFromCart(userId, prodName);
        return "product removed from cart of userId" + userId + "successfully";
    }

    @PostMapping("/customer/addToMyCart")
    public String addToMyCart(
            @RequestHeader("X-UserId") int userId,
            @RequestParam @NotBlank(message = "Product name cannot be blank") String prodName,
            @RequestParam @Positive(message = "Quantity must be positive") int quantity) {
        cartService.addToCart(userId, prodName, quantity);
        return "Product with product name " + prodName + " added to cart with userId " + userId;
    }

    @GetMapping("/customer/getMyCart") //	 route for logged user
    public Cart getMyCart(@RequestHeader("X-UserId") int userId) {
        System.out.println("**getCartByUser userId = " + userId);
        return cartService.getMyCart(userId);
    }

    @DeleteMapping("/customer/clearMyCart")
    public String clearMyCartContents(@RequestHeader("X-UserId") int userId){
        cartService.clearCartContentsOnly(userId);
        return "Cart contents cleared successfully for user.";
    }

    @DeleteMapping("/customer/removeItemFromMyCart")
    public String removeFromMyCart(
            @RequestHeader("X-UserId") int userId,
            @RequestParam @NotBlank(message = "Product name cannot be blank") String prodName) {
        cartService.removeItemFromCart(userId, prodName);
        return "Item removed from cart successfully.";
    }


    @GetMapping("/biller/getCartByUser/{userId}")
    public Cart getCartByUser(@PathVariable @Min(value = 1, message = "User ID must be positive") int userId) {

        return cartService.getCartByUserId(userId);
    }

    @PutMapping("/customer/increaseQuantity")
    public String increaseQuantity(
            @RequestHeader("X-UserId") int userId,
            @RequestParam @NotBlank(message = "Product name cannot be blank") String prodName) {
        cartService.increaseQuantity(userId, prodName);
        return "Increased item quantity by one";
    }

    @PutMapping("/customer/decreaseQuantity")
    public String decreaseQuantity(
            @RequestHeader("X-UserId") int userId,
            @RequestParam @NotBlank(message = "Product name cannot be blank") String prodName) {
        cartService.decreaseQuantity(userId, prodName);
        return "Decreased item quantity by one";
    }
    

    @PutMapping("/biller/increaseQuantity")
    public String increaseQuantityFromUserCart(
            @RequestParam @Min(value = 1, message = "User ID must be positive") int userId,
            @RequestParam @NotBlank(message = "Product name cannot be blank") String prodName) {
        cartService.increaseQuantity(userId, prodName);
        return "Increased item quantity by one";
    }

    @PutMapping("/biller/decreaseQuantity")
    public String decreaseQuantityFromUserCart(
            @RequestParam @Min(value = 1, message = "User ID must be positive") int userId,
            @RequestParam @NotBlank(message = "Product name cannot be blank") String prodName) {
        cartService.decreaseQuantity(userId, prodName);
        return "Decreased item quantity by one";
    }

    @DeleteMapping("/biller/clearUserCartContents/{userId}")
    public String clearUserCartContents(@PathVariable @Min(value = 1, message = "User ID must be positive") int userId){
        cartService.clearCartContentsOnly(userId);
        return "Cart contents cleared successfully for user.";
    }



    @DeleteMapping("/clearCartAndReduceStock/{userId}")
    public String clearCartAndReduceStockForUser(@PathVariable @Min(value = 1, message = "User ID must be positive") int userId) {
        cartService.clearCart(userId);
        return "Cart cleared successfully.";
    }

    @DeleteMapping("/deleteCart/{cartId}")
    public String deleteCart(@PathVariable @Min(value = 1, message = "Cart ID must be positive") int cartId) {
        cartService.deleteCart(cartId);
        return "Cart deleted successfully";
    }


    @GetMapping("/getCartIdByUserId/{userId}")
    public int getCartIdByUserId(@PathVariable @Min(value = 1, message = "User ID must be positive") int userId) {
        return cartService.getCartIdByUserId(userId);
    }

    @GetMapping("/getCartItemsByUserId/{userId}")
    public List<CartItems> getCartItemsByUserId(@PathVariable @Min(value = 1, message = "User ID must be positive") int userId) {
        return cartService.getCartItemsByUserId(userId);
    }

}
