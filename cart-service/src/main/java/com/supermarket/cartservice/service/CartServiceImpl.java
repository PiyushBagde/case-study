package com.supermarket.cartservice.service;

import com.supermarket.cartservice.dto.ProductResponse;
import com.supermarket.cartservice.exception.CartOperationException;
import com.supermarket.cartservice.exception.OperationFailedException;
import com.supermarket.cartservice.exception.ResourceNotFoundException;
import com.supermarket.cartservice.feign.InventoryServiceClient;
import com.supermarket.cartservice.model.Cart;
import com.supermarket.cartservice.model.CartItems;
import com.supermarket.cartservice.repository.CartItemsRepository;
import com.supermarket.cartservice.repository.CartRepository;
import feign.FeignException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CartServiceImpl implements CartService{
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemsRepository cartItemsRepository;

    @Autowired
    private InventoryServiceClient inventoryServiceClient;

    @Override
    public double calculateNewTotal(double currentTotal, double itemPriceChange, boolean increase) {
        return increase ? currentTotal + itemPriceChange : currentTotal - itemPriceChange;
    }

    @Override
    @Transactional
    public void addToCart(int userId, String prodName, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive.");
        }
        ProductResponse product;

        try {
            product = inventoryServiceClient.getProductByProdName(prodName);
        }
        catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Product '" + prodName + "' not found in inventory.", e);
        } catch (FeignException e) {
            throw new OperationFailedException("Failed to retrieve product details from inventory service.", e);
        } catch (Exception e) {
            // Catch any other unexpected error
            throw new OperationFailedException("An unexpected error occurred while contacting inventory service.", e);
        }

        // if available stock
        if (product.getStock() < quantity) {
            System.out.println("inside stock check");
            throw new CartOperationException("Insufficient stock for product '" + prodName + "'. Available: " + product.getStock());
        }

        int prodId = product.getProdId();
        double prodPrice = product.getPrice();
        double totalPrice = prodPrice * quantity;

        // find or create new cart
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            // if no cart found for userId, creating new one
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            newCart.setCartTotalPrice(0.0);
            try {
                return cartRepository.save(newCart);
            } catch (DataAccessException dae) {
                throw new OperationFailedException("Failed to create a new cart for user: " + userId, dae);
            }
        });

        // Check if item already exists in cart, update quantity if it does
        Optional<CartItems> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProdId() == prodId)
                .findFirst();

        CartItems itemToSave;
        double cartTotalChange;
        if (existingItemOpt.isPresent()) { // already existing cartItem
            itemToSave = existingItemOpt.get();
            int newQuantity = itemToSave.getQuantity() + quantity;
            // Check stock for the *additional* quantity
            ProductResponse currentStockCheck;
            try {
                currentStockCheck = inventoryServiceClient.getProductById(prodId);
            } catch (FeignException.NotFound e) {
                throw new ResourceNotFoundException("Product '" + prodName + "' not found in inventory.", e);
            }
            if (currentStockCheck.getStock() < newQuantity) {
                throw new CartOperationException("Insufficient stock for product '" + prodName + "'. Available: " + currentStockCheck.getStock());
            }
            itemToSave.setQuantity(newQuantity);
            itemToSave.setTotalPrice(prodPrice * newQuantity); // Recalculate total price for item
        } else {
            itemToSave = new CartItems();
            itemToSave.setCart(cart);
            itemToSave.setProdId(prodId);
            itemToSave.setProdName(prodName);
            itemToSave.setQuantity(quantity);
            itemToSave.setPrice(prodPrice); // Price per unit at the time of adding
            itemToSave.setTotalPrice(totalPrice);
            cart.getItems().add(itemToSave); // Add to the list in the Cart object
        }
        cartTotalChange = totalPrice;

        try {
            cartItemsRepository.save(itemToSave);
            double newCartTotal = calculateNewTotal(cart.getCartTotalPrice(), cartTotalChange, true);
            cart.setCartTotalPrice(newCartTotal);
            cartRepository.save(cart);
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to save item or update cart for user: " + userId, e);
        } catch (Exception e) {
            throw new OperationFailedException("An unexpected error occurred while updating the cart for user: " + userId, e);
        }
    }

    @Override
    public Cart getCartByUserId(int userId) {
        return cartRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("No cart available for user ID: " + userId));
    }

    //increase quantity
    @Override
    @Transactional
    public void increaseQuantity(int userId, String prodName) {
        Cart cart = getCartByUserId(userId); // Throws ResourceNotFoundException if cart not found
        AtomicBoolean itemFound = new AtomicBoolean(false);

        cart.getItems().stream()
                .filter(item -> item.getProdName().equalsIgnoreCase(prodName))
                .findFirst()
                .ifPresentOrElse(item -> {
                            itemFound.set(true);
                            // Check inventory stock before increasing
                            try {
                                ProductResponse p = inventoryServiceClient.getProductById(item.getProdId());
                                if (p.getStock() <= item.getQuantity()) {
                                    throw new CartOperationException("Insufficient stock for product '" + prodName + "'. Available: " + p.getStock());
                                }
                            } catch (FeignException.NotFound e) {
                                throw new ResourceNotFoundException("Product '" + prodName + "' not found in inventory.", e);
                            }
                            int newQuantity = item.getQuantity() + 1;
                            item.setQuantity(newQuantity);
                            double itemPrice = item.getPrice();
                            item.setTotalPrice(itemPrice * newQuantity); // Update item total price

                            double cartTotal = calculateNewTotal(cart.getCartTotalPrice(), itemPrice, true); // Increase cart total by one
                            cart.setCartTotalPrice(cartTotal);

                            try {
                                cartItemsRepository.save(item); // Save the updated item
                                cartRepository.save(cart);    // Save the updated cart total
                            } catch (DataAccessException e) {
                                throw new OperationFailedException("Failed to update item quantity or cart total for product: " + prodName, e);
                            }
                        },
                        //  Throw if item not found
                        () -> {
                            throw new ResourceNotFoundException("Item '" + prodName + "' not found in the cart for user: " + userId);
                        });
    }

    // decrease quantity
    @Override
    @Transactional
    public void decreaseQuantity(int userId, String prodName) {
        Cart cart = getCartByUserId(userId);
        AtomicBoolean itemProcessed = new AtomicBoolean(false);

        cart.getItems().stream()
                .filter(item -> item.getProdName().equalsIgnoreCase(prodName))
                .findFirst()
                .ifPresentOrElse(item -> {
                    itemProcessed.set(true);
                    int currentQuantity = item.getQuantity();

                    if (currentQuantity <= 1) {
                        // If quantity is 1 or less, remove the item instead of decreasing
                        removeItemFromCartInternal(cart, item);
                    } else {
                        int newQuantity = currentQuantity - 1;
                        item.setQuantity(newQuantity);
                        double itemPrice = item.getPrice();
                        item.setTotalPrice(itemPrice * newQuantity); // Update item total price

                        double cartTotal = calculateNewTotal(cart.getCartTotalPrice(), itemPrice, false); // Decrease cart total
                        cart.setCartTotalPrice(cartTotal);

                        try {
                            cartItemsRepository.save(item);
                            cartRepository.save(cart);
                        } catch (DataAccessException e) {
                            throw new OperationFailedException("Failed to update item quantity or cart total for product: " + prodName, e);
                        }
                    }
                }, () -> {
                    throw new ResourceNotFoundException("Item '" + prodName + "' not found in the cart for user: " + userId);
                });
    }

    // Remove item from cart
    @Override
    public void removeItemFromCartInternal(Cart cart, CartItems item) {
        double updatedPrice = calculateNewTotal(cart.getCartTotalPrice(), item.getTotalPrice(), false); // Decrease by item's total
        cart.setCartTotalPrice(updatedPrice);
        cart.getItems().remove(item); // Remove from collection in Cart object

        try {
            cartItemsRepository.delete(item); // Delete from DB
            cartRepository.save(cart);        // Save updated cart total
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to remove item '" + item.getProdName() + "' from cart.", e);
        }
    }

    @Override
    @Transactional
    public void removeItemFromCart(int userId, String prodName) {
        Cart cart = getCartByUserId(userId);
        CartItems item = cartItemsRepository.findByCartCartIdAndProdName(cart.getCartId(), prodName).orElseThrow(() -> new ResourceNotFoundException("Item '" + prodName + "' not found in cart for user: " + userId));
        removeItemFromCartInternal(cart, item); // Use internal helper
    }

    @Override
    @Transactional
    public void clearCart(int userId) {
    	
    	if (userId <= 0) {
            throw new IllegalArgumentException("Invalid User ID in header for clearing cart.");
    	}
    	
        Cart cart = getCartByUserId(userId);
        List<CartItems> itemsList = cart.getItems();
        if (cart.getCartTotalPrice() != 0.0) {
            cart.setCartTotalPrice(0.0);
            try {
                cartRepository.save(cart);
            } catch (DataAccessException e) {
                throw new OperationFailedException("Failed to clear cart for user: " + userId, e);
            }
        }

        for (CartItems item : itemsList) {
            try {
                inventoryServiceClient.reduceStock(item.getProdId(), item.getQuantity());
            } catch (FeignException e) {
                // Translate specific Feign errors if needed
                if (e instanceof FeignException.NotFound) {
                    throw new OperationFailedException("Error during cart clear: Product ID " + item.getProdId() + " not found in inventory.", e);
                }
                throw new OperationFailedException("Failed to update inventory for product ID " + item.getProdId() + " while clearing cart.", e);
            } catch (Exception e) {
                throw new OperationFailedException("An unexpected error occurred while updating inventory for product ID " + item.getProdId(), e);
            }
        }

        try {
            //  delete all items belonging to the cart
            cartItemsRepository.deleteByCart_CartId(cart.getCartId());

            // Clear the list in the Cart object and reset total price
            cart.getItems().clear(); // Clear the collection managed
            cart.setCartTotalPrice(0.0);
            cartRepository.save(cart); // Save the cart with empty items list and zero total
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to clear cart items from database after updating inventory.", e);
        }
    }

    @Override
    @Transactional
    public void clearCartContentsOnly(int userId) {
        if(userId <= 0) {
            throw new IllegalArgumentException("Invalid User ID");
        }
        Cart cart = getCartByUserId(userId);

        if (cart.getItems().isEmpty() && cart.getCartTotalPrice() == 0.0) {
            return;
        }
        try {
           cartItemsRepository.deleteByCart_CartId(cart.getCartId());
            cart.getItems().clear();
            cart.setCartTotalPrice(0.0);
            cartRepository.save(cart);
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to clear cart items from database.", e);
        }
    }

    public int getCartIdByUserId(int userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Cart not found for user ID: " + userId));
        return cart.getCartId();
    }

    @Override
    @Transactional
    public void deleteCart(int cartId) {
        // Check if cart exists first to provide a better error message
        if (!cartRepository.existsById(cartId)) {
            throw new ResourceNotFoundException("Cannot delete. Cart not found with ID: " + cartId);
        }
        try {
            // Delete associated items first due to potential foreign key constraints
            cartItemsRepository.deleteByCart_CartId(cartId);
            cartRepository.deleteById(cartId);
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to delete cart with ID: " + cartId, e);
        }
    }

    @Override
    public Cart getMyCart(int userId) {
        return getCartByUserId(userId);
    }

    @Override
    public List<CartItems> getCartItemsByUserId(int userId) {
        Cart cart;
        cart = cartRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("No cart found for user ID: " + userId + " to retrieve items."));
        return cart.getItems();
    }
}
