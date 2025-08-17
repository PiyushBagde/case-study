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
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    private final int userId = 1;
    private final int cartId = 10;
    private final String prod1Name = "Laptop";
    private final int prod1Id = 101;
    private final double prod1Price = 1200.0;
    private final String prod2Name = "Keyboard";
    private final int prod2Id = 102;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemsRepository cartItemsRepository;
    @Mock
    private InventoryServiceClient inventoryServiceClient;
    @InjectMocks
    private CartServiceImpl cartService;
    @Captor
    private ArgumentCaptor<Cart> cartCaptor;
    @Captor
    private ArgumentCaptor<CartItems> cartItemsCaptor;
    private Cart sampleCart;
    private CartItems sampleCartItem1;
    private CartItems sampleCartItem2;
    private ProductResponse sampleProduct1Response;
    private ProductResponse sampleProduct2Response;

    @BeforeEach
    void setUp() {
        sampleCart = new Cart(cartId, userId, 0.0, new ArrayList<>()); // Start with empty items list
        sampleProduct1Response = new ProductResponse(prod1Id, prod1Name, prod1Price, 50);
        sampleProduct2Response = new ProductResponse(prod2Id, prod2Name, 75.0, 100);
        // Sample items
        sampleCartItem1 = new CartItems(1, sampleCart, prod1Id, prod1Name, prod1Price, 1, prod1Price);
        sampleCartItem2 = new CartItems(2, sampleCart, prod2Id, prod2Name, 75.0, 2, 150.0);
    }

    // Helper to create a FeignException.NotFound
    private FeignException.NotFound createFeignNotFoundException() {
        Request request = Request.create(Request.HttpMethod.GET, "/dummy", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.NotFound("Not Found", request, null, Collections.emptyMap());
    }

    // --- addToCart Tests ---
    @Test
    @DisplayName("AddToCart: Success creates new cart and adds new item")
    void addToCart_NewCartNewItem_Success() {

        int quantity = 2;
        double expectedItemTotal = prod1Price * quantity;
        when(inventoryServiceClient.getProductByProdName(prod1Name)).thenReturn(sampleProduct1Response);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        when(cartRepository.save(cartCaptor.capture())).thenAnswer(invocation -> {
            Cart cartBeingSaved = invocation.getArgument(0);
            if (cartBeingSaved.getCartId() == 0) {
                cartBeingSaved.setCartId(cartId);
            }
            return cartBeingSaved;
        });
        when(cartItemsRepository.save(any(CartItems.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        cartService.addToCart(userId, prod1Name, quantity);

        // Assert
        verify(inventoryServiceClient).getProductByProdName(prod1Name);
        verify(cartRepository).findByUserId(userId);

        // Verify save was called twice
        verify(cartRepository, times(2)).save(any(Cart.class));
        List<Cart> capturedCarts = cartCaptor.getAllValues();

        Cart finalCartState = capturedCarts.get(1);
        assertEquals(userId, finalCartState.getUserId());
        assertEquals(cartId, finalCartState.getCartId());
        assertEquals(expectedItemTotal, finalCartState.getCartTotalPrice()); // Check the final total

        // Verify item save
        verify(cartItemsRepository).save(cartItemsCaptor.capture());
    }

    @Test
    @DisplayName("AddToCart: Success adds new item to existing cart")
    void addToCart_ExistingCartNewItem_Success() {
        // Arrange
        int quantity = 1;
        sampleCart.setCartTotalPrice(150.0); // Assume cart already had item 2
        sampleCart.getItems().add(sampleCartItem2);

        when(inventoryServiceClient.getProductByProdName(prod1Name)).thenReturn(sampleProduct1Response); // Stock 50 > 1
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));
        when(cartItemsRepository.save(any(CartItems.class))).thenAnswer(i -> i.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart); // Mock the final save

        // Act
        cartService.addToCart(userId, prod1Name, quantity);

        // Assert
        verify(inventoryServiceClient).getProductByProdName(prod1Name);
        verify(cartRepository).findByUserId(userId);
        verify(cartRepository).save(cartCaptor.capture()); // Only one save expected here
        assertEquals(150.0 + (prod1Price * quantity), cartCaptor.getValue().getCartTotalPrice()); // Verify correct new total

        verify(cartItemsRepository).save(cartItemsCaptor.capture());
        CartItems savedItem = cartItemsCaptor.getValue();
        assertEquals(prod1Id, savedItem.getProdId());
        assertEquals(quantity, savedItem.getQuantity());
        assertEquals(prod1Price * quantity, savedItem.getTotalPrice());
        assertEquals(cartId, savedItem.getCart().getCartId());
        // Verify the item was added conceptually (mock setup implies this)
    }


    @Test
    @DisplayName("AddToCart: Success updates quantity of existing item")
    void addToCart_ExistingCartExistingItem_Success() {
        // Arrange
        int initialQuantity = 1;
        int quantityToAdd = 2;
        int finalQuantity = initialQuantity + quantityToAdd;
        double initialItemTotal = prod1Price * initialQuantity;
        double initialCartTotal = initialItemTotal; // Cart only has this item initially

        sampleCartItem1.setQuantity(initialQuantity);
        sampleCartItem1.setTotalPrice(initialItemTotal);
        sampleCart.getItems().add(sampleCartItem1);
        sampleCart.setCartTotalPrice(initialCartTotal);

        when(inventoryServiceClient.getProductByProdName(prod1Name)).thenReturn(sampleProduct1Response); // Stock = 50
        when(inventoryServiceClient.getProductById(prod1Id)).thenReturn(sampleProduct1Response); // Stock check for update
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));
        when(cartItemsRepository.save(any(CartItems.class))).thenAnswer(i -> i.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);

        // Act
        cartService.addToCart(userId, prod1Name, quantityToAdd);

        // Assert
        verify(inventoryServiceClient).getProductByProdName(prod1Name);
        verify(inventoryServiceClient).getProductById(prod1Id);
        verify(cartRepository).findByUserId(userId);
        verify(cartRepository).save(cartCaptor.capture());
        assertEquals(initialCartTotal + (prod1Price * quantityToAdd), cartCaptor.getValue().getCartTotalPrice());

        verify(cartItemsRepository).save(cartItemsCaptor.capture());
        assertEquals(finalQuantity, cartItemsCaptor.getValue().getQuantity());
        assertEquals(prod1Price * finalQuantity, cartItemsCaptor.getValue().getTotalPrice());
    }


    @Test
    @DisplayName("AddToCart: Throws IllegalArgumentException for zero quantity")
    void addToCart_ZeroQuantity_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            cartService.addToCart(userId, prod1Name, 0);
        });
        verifyNoInteractions(inventoryServiceClient, cartRepository, cartItemsRepository);
    }

    @Test
    @DisplayName("AddToCart: Throws ResourceNotFoundException if product not in inventory")
    void addToCart_ProductNotFound_ThrowsResourceNotFoundException() {
        when(inventoryServiceClient.getProductByProdName(prod1Name)).thenThrow(createFeignNotFoundException());
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.addToCart(userId, prod1Name, 1);
        });
        verify(inventoryServiceClient).getProductByProdName(prod1Name);
        verifyNoInteractions(cartRepository, cartItemsRepository);
    }

    @Test
    @DisplayName("AddToCart: Throws CartOperationException for insufficient stock")
    void addToCart_InsufficientStock_ThrowsCartOperationException() {
        sampleProduct1Response.setStock(1);// Only 1 in stock
        when(inventoryServiceClient.getProductByProdName(prod1Name)).thenReturn(sampleProduct1Response);

        assertThrows(CartOperationException.class, () -> {
            cartService.addToCart(userId, prod1Name, 2); // Requesting 2
        });
        verify(inventoryServiceClient).getProductByProdName(prod1Name);
        verifyNoInteractions(cartRepository, cartItemsRepository);
    }


    // --- getCartByUserId Tests ---
    @Test
    @DisplayName("GetCartByUserId: Success returns cart")
    void getCartByUserId_IfExists_ReturnsCart() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));
        Cart result = cartService.getCartByUserId(userId);
        assertNotNull(result);
        assertEquals(cartId, result.getCartId());
        verify(cartRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("GetCartByUserId: Throws ResourceNotFoundException if not exists")
    void getCartByUserId_IfNotExists_ThrowsResourceNotFoundException() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.getCartByUserId(userId);
        });
        verify(cartRepository).findByUserId(userId);
    }

    // --- increaseQuantity Tests ---
    @Test
    @DisplayName("IncreaseQuantity: Success increases quantity and totals")
    void increaseQuantity_WhenItemExistsAndStockSufficient_Success() {
        // Arrange
        sampleCart.getItems().add(sampleCartItem1); // Item with qty 1, price 1200
        sampleCart.setCartTotalPrice(sampleCartItem1.getTotalPrice()); // 1200
        ProductResponse stockCheckResponse = new ProductResponse(prod1Id, prod1Name, prod1Price, 10); // Enough stock

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));
        when(inventoryServiceClient.getProductById(prod1Id)).thenReturn(stockCheckResponse);
        when(cartItemsRepository.save(any(CartItems.class))).thenAnswer(i -> i.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);

        // Act
        cartService.increaseQuantity(userId, prod1Name);

        // Assert
        verify(cartRepository).findByUserId(userId);
        verify(inventoryServiceClient).getProductById(prod1Id);
        verify(cartItemsRepository).save(cartItemsCaptor.capture());
        verify(cartRepository).save(cartCaptor.capture());

        assertEquals(2, cartItemsCaptor.getValue().getQuantity()); // Qty increased
        assertEquals(prod1Price * 2, cartItemsCaptor.getValue().getTotalPrice()); // Item total updated
        assertEquals(prod1Price * 2, cartCaptor.getValue().getCartTotalPrice()); // Cart total updated
    }

    @Test
    @DisplayName("IncreaseQuantity: Throws ResourceNotFoundException if item not in cart")
    void increaseQuantity_WhenItemNotInCart_ThrowsResourceNotFoundException() {
        // Arrange - Cart exists but is empty or doesn't contain prod1Name
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart)); // Empty cart

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.increaseQuantity(userId, prod1Name);
        });
        verify(cartRepository).findByUserId(userId);
        verifyNoInteractions(inventoryServiceClient, cartItemsRepository); // No stock check or save if item not found
    }

    @Test
    @DisplayName("IncreaseQuantity: Throws CartOperationException if stock insufficient")
    void increaseQuantity_WhenStockInsufficient_ThrowsCartOperationException() {
        // Arrange
        sampleCart.getItems().add(sampleCartItem1); // Item with qty 1
        sampleCart.setCartTotalPrice(sampleCartItem1.getTotalPrice());
        ProductResponse stockCheckResponse = new ProductResponse(prod1Id, prod1Name, prod1Price, 1); // Only 1 in stock

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));
        when(inventoryServiceClient.getProductById(prod1Id)).thenReturn(stockCheckResponse); // Stock check fails

        // Act & Assert
        assertThrows(CartOperationException.class, () -> {
            cartService.increaseQuantity(userId, prod1Name);
        });
        verify(cartRepository).findByUserId(userId);
        verify(inventoryServiceClient).getProductById(prod1Id);
        verify(cartItemsRepository, never()).save(any()); // Save not called
        verify(cartRepository, never()).save(cartCaptor.capture()); // Cart total save not called
    }


    // --- decreaseQuantity Tests ---
    @Test
    @DisplayName("DecreaseQuantity: Success decreases quantity > 1")
    void decreaseQuantity_WhenQuantityGreaterThanOne_Success() {
        // Arrange
        sampleCartItem1.setQuantity(3); // Start with 3
        sampleCartItem1.setTotalPrice(prod1Price * 3);
        sampleCart.getItems().add(sampleCartItem1);
        sampleCart.setCartTotalPrice(prod1Price * 3);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));
        when(cartItemsRepository.save(any(CartItems.class))).thenAnswer(i -> i.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);

        // Act
        cartService.decreaseQuantity(userId, prod1Name);

        // Assert
        verify(cartRepository).findByUserId(userId);
        verify(cartItemsRepository).save(cartItemsCaptor.capture());
        verify(cartRepository).save(cartCaptor.capture());

        assertEquals(2, cartItemsCaptor.getValue().getQuantity());
        assertEquals(prod1Price * 2, cartItemsCaptor.getValue().getTotalPrice());
        assertEquals(prod1Price * 2, cartCaptor.getValue().getCartTotalPrice());
        verify(cartItemsRepository, never()).delete(any()); // Delete should not be called
    }

    @Test
    @DisplayName("DecreaseQuantity: Success removes item when quantity is 1")
    void decreaseQuantity_WhenQuantityIsOne_RemovesItem() {
        // Arrange
        sampleCartItem1.setQuantity(1); // Start with 1
        sampleCartItem1.setTotalPrice(prod1Price);
        sampleCart.getItems().add(sampleCartItem1);
        sampleCart.setCartTotalPrice(prod1Price);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));
        doNothing().when(cartItemsRepository).delete(any(CartItems.class)); // Mock void delete
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);

        // Act
        cartService.decreaseQuantity(userId, prod1Name);

        // Assert
        verify(cartRepository).findByUserId(userId);
        verify(cartItemsRepository).delete(sampleCartItem1); // Verify delete was called with the correct item
        verify(cartRepository).save(cartCaptor.capture());
        assertEquals(0.0, cartCaptor.getValue().getCartTotalPrice()); // Cart total should be 0
        verify(cartItemsRepository, never()).save(any(CartItems.class)); // Item save should not be called
        assertTrue(sampleCart.getItems().isEmpty()); // Check item removed from list in memory object (though verification of delete is key)

    }

    @Test
    @DisplayName("DecreaseQuantity: Throws ResourceNotFoundException if item not in cart")
    void decreaseQuantity_WhenItemNotInCart_ThrowsResourceNotFoundException() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart)); // Empty cart

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.decreaseQuantity(userId, prod1Name);
        });
        verify(cartRepository).findByUserId(userId);
        verifyNoInteractions(cartItemsRepository); // No save or delete
    }


    // --- removeItemFromCart Tests ---
    @Test
    @DisplayName("RemoveItemFromCart: Success removes item")
    void removeItemFromCart_WhenItemExists_RemovesItemAndUpdatesTotal() {
        // Arrange
        sampleCart.getItems().add(sampleCartItem1);
        sampleCart.setCartTotalPrice(sampleCartItem1.getTotalPrice()); // 1200

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));
        when(cartItemsRepository.findByCartCartIdAndProdName(cartId, prod1Name)).thenReturn(Optional.of(sampleCartItem1));
        doNothing().when(cartItemsRepository).delete(sampleCartItem1);
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);

        // Act
        cartService.removeItemFromCart(userId, prod1Name);

        // Assert
        verify(cartRepository).findByUserId(userId);
        verify(cartItemsRepository).findByCartCartIdAndProdName(cartId, prod1Name);
        verify(cartItemsRepository).delete(sampleCartItem1);
        verify(cartRepository).save(cartCaptor.capture());
        assertEquals(0.0, cartCaptor.getValue().getCartTotalPrice()); // Cart total should be 0
        assertTrue(sampleCart.getItems().isEmpty());
    }

    @Test
    @DisplayName("RemoveItemFromCart: Throws ResourceNotFoundException if item not found")
    void removeItemFromCart_WhenItemNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart)); // Cart exists
        when(cartItemsRepository.findByCartCartIdAndProdName(cartId, prod1Name)).thenReturn(Optional.empty()); // Item not found

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.removeItemFromCart(userId, prod1Name);
        });
        verify(cartRepository).findByUserId(userId);
        verify(cartItemsRepository).findByCartCartIdAndProdName(cartId, prod1Name);
        verify(cartItemsRepository, never()).delete(any());
        verify(cartRepository, never()).save(any());
    }

    // --- clearCart Tests ---
    @Test
    @DisplayName("ClearCart: Success clears items, updates inventory, resets total")
    void clearCart_WhenItemsExist_ClearsCartAndUpdatesInventory() {
        // Arrange
        sampleCart.getItems().add(sampleCartItem1); // Qty 1
        sampleCart.getItems().add(sampleCartItem2); // Qty 2
        sampleCart.setCartTotalPrice(sampleCartItem1.getTotalPrice() + sampleCartItem2.getTotalPrice());

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));
        // Mock inventory calls - assume they succeed
        doNothing().when(inventoryServiceClient).reduceStock(sampleCartItem1.getProdId(), sampleCartItem1.getQuantity());
        doNothing().when(inventoryServiceClient).reduceStock(sampleCartItem2.getProdId(), sampleCartItem2.getQuantity());
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);

        // Act
        cartService.clearCart(userId);

        // Assert
        verify(cartRepository).findByUserId(userId);
        // Verify inventory updated for EACH item
        verify(inventoryServiceClient).reduceStock(sampleCartItem1.getProdId(), sampleCartItem1.getQuantity());
        verify(inventoryServiceClient).reduceStock(sampleCartItem2.getProdId(), sampleCartItem2.getQuantity());
        // Verify items deleted and cart saved
        verify(cartItemsRepository).deleteByCart_CartId(cartId);
        verify(cartRepository, times(2)).save(cartCaptor.capture()); // Once for initial total reset, once for final save
        assertEquals(0.0, cartCaptor.getValue().getCartTotalPrice()); // Final total is 0
        assertTrue(cartCaptor.getValue().getItems().isEmpty()); // Items list cleared
    }

    @Test
    @DisplayName("ClearCart: Throws OperationFailedException if reduceStock fails")
    void clearCart_WhenReduceStockFails_ThrowsOperationFailedException() {
        // Arrange
        sampleCart.getItems().add(sampleCartItem1);
        sampleCart.setCartTotalPrice(sampleCartItem1.getTotalPrice());

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));
        // Simulate inventory failure
        doThrow(createFeignNotFoundException()).when(inventoryServiceClient).reduceStock(anyInt(), anyInt());

        // Act & Assert
        assertThrows(OperationFailedException.class, () -> {
            cartService.clearCart(userId);
        });

        verify(cartRepository).findByUserId(userId);
        verify(inventoryServiceClient).reduceStock(sampleCartItem1.getProdId(), sampleCartItem1.getQuantity()); // Called once before failing
        verify(cartItemsRepository, never()).deleteByCart_CartId(anyInt()); // Deletion should not happen
        verify(cartRepository, times(1)).save(any()); // Only initial total reset save might happen
    }


    // --- deleteCart Tests ---
    @Test
    @DisplayName("DeleteCart: Success deletes cart and items")
    void deleteCart_WhenCartExists_DeletesCartAndItems() {
        // Arrange
        int cartIdToDelete = cartId;
        when(cartRepository.existsById(cartIdToDelete)).thenReturn(true);
        doNothing().when(cartRepository).deleteById(cartIdToDelete);

        // Act
        assertDoesNotThrow(() -> cartService.deleteCart(cartIdToDelete));

        // Assert
        verify(cartRepository).existsById(cartIdToDelete);
        verify(cartItemsRepository).deleteByCart_CartId(cartIdToDelete);
        verify(cartRepository).deleteById(cartIdToDelete);
    }

    @Test
    @DisplayName("DeleteCart: Throws ResourceNotFoundException if cart not exists")
    void deleteCart_WhenCartNotExists_ThrowsResourceNotFoundException() {
        // Arrange
        int cartIdToDelete = 999;
        when(cartRepository.existsById(cartIdToDelete)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.deleteCart(cartIdToDelete);
        });
        verify(cartRepository).existsById(cartIdToDelete);
        verifyNoInteractions(cartItemsRepository);
        verify(cartRepository, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("DeleteCart: Throws OperationFailedException on DB error")
    void deleteCart_WhenDbError_ThrowsOperationFailedException() {
        // Arrange
        int cartIdToDelete = cartId;
        when(cartRepository.existsById(cartIdToDelete)).thenReturn(true);
        // Simulate error during item deletion
        doThrow(new DataAccessException("DB error") {
        }).when(cartItemsRepository).deleteByCart_CartId(cartIdToDelete);

        // Act & Assert
        assertThrows(OperationFailedException.class, () -> {
            cartService.deleteCart(cartIdToDelete);
        });
        verify(cartRepository).existsById(cartIdToDelete);
        verify(cartItemsRepository).deleteByCart_CartId(cartIdToDelete);
        verify(cartRepository, never()).deleteById(anyInt()); // Cart itself not deleted
    }

    // --- getCartItemsByUserId Tests ---
    @Test
    @DisplayName("GetCartItemsByUserId: Success returns items list")
    void getCartItemsByUserId_WhenCartExistsWithItems_ReturnsList() {
        // Arrange
        sampleCart.getItems().add(sampleCartItem1);
        sampleCart.getItems().add(sampleCartItem2);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));

        // Act
        List<CartItems> result = cartService.getCartItemsByUserId(userId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(cartRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("GetCartItemsByUserId: Throws ResourceNotFoundException if cart not found")
    void getCartItemsByUserId_WhenCartNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.getCartItemsByUserId(userId);
        });
        verify(cartRepository).findByUserId(userId);
    }

}