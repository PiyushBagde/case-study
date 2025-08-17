package com.supermarket.billingservice.service;

import com.supermarket.billingservice.dto.CartItemResponse;
import com.supermarket.billingservice.exception.OperationFailedException;
import com.supermarket.billingservice.exception.OrderPlacementException;
import com.supermarket.billingservice.exception.ResourceNotFoundException;
import com.supermarket.billingservice.feign.CartServiceClient;
import com.supermarket.billingservice.model.Order;
import com.supermarket.billingservice.model.OrderItems;
import com.supermarket.billingservice.repository.OrderRepository;
import com.supermarket.billingservice.repository.OrderitemsRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderitemsRepository orderitemsRepository;
    @Mock
    private CartServiceClient cartServiceClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;
    @Captor
    private ArgumentCaptor<List<OrderItems>> orderItemsListCaptor;

    private final int userId = 1;
    private final int cartId = 10;
    private final int orderId = 100;
    private List<CartItemResponse> sampleCartItems;
    private CartItemResponse item1;
    private CartItemResponse item2;

    @BeforeEach
    void setUp() {
        item1 = new CartItemResponse(1, cartId, 101, "Laptop", 1200.00, 1, 1200.00);
        item2 = new CartItemResponse(2, cartId, 102, "Keyboard", 75.00, 2, 150.00);
        sampleCartItems = List.of(item1, item2);
    }

    // Helper to create a FeignException.NotFound
    private FeignException.NotFound createFeignNotFoundException() {
        Request request = Request.create(Request.HttpMethod.GET, "/dummy", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.NotFound("Not Found", request, null, Collections.emptyMap());
    }
    // Helper to create a generic FeignException (e.g., 500 error)
    private FeignException createFeignServerErrorException() {
        Request request = Request.create(Request.HttpMethod.GET, "/dummy", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.InternalServerError("Server Error", request, null, Collections.emptyMap());
    }


    // --- placeOrder Tests ---

    @Test
    @DisplayName("PlaceOrder: Success creates order and clears cart")
    void placeOrder_WhenCartNotEmptyAndServicesOk_ShouldCreateOrderAndClearCart() {
        // Arrange
        double expectedTotal = item1.getTotalPrice() + item2.getTotalPrice(); // 1200 + 150 = 1350
        Order expectedSavedOrder = new Order(orderId, userId, cartId, null, expectedTotal, new ArrayList<>()); // Date set later

        when(cartServiceClient.getCartItemsByUserId(userId)).thenReturn(sampleCartItems);
        when(cartServiceClient.getCartIdByUserId(userId)).thenReturn(cartId);
        // Mock saving the order
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setOrderId(orderId); // Simulate ID generation
            o.setOrderDate(LocalDateTime.now()); // Simulate date set
            return o;
        });
        // Mock saving order items
        when(orderitemsRepository.saveAll(any(List.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Order result = orderService.placeOrder(userId);

        // Assert
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(userId, result.getUserId());
        assertEquals(cartId, result.getCartId());
        assertEquals(expectedTotal, result.getTotalBillPrice());
        assertNotNull(result.getOrderDate());
        assertEquals(2, result.getOrderItems().size()); // Check items are associated back

        // Verify interactions
        verify(cartServiceClient).getCartItemsByUserId(userId);
        verify(cartServiceClient).getCartIdByUserId(userId);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(expectedTotal, orderCaptor.getValue().getTotalBillPrice()); // Verify total passed to save

        verify(orderitemsRepository).saveAll(orderItemsListCaptor.capture());
        List<OrderItems> savedItems = orderItemsListCaptor.getValue();
        assertEquals(2, savedItems.size());
        // Check items details and ensure order is linked back
        assertNotNull(savedItems.get(0).getOrder());
        assertEquals(orderId, savedItems.get(0).getOrder().getOrderId());
        assertEquals(item1.getProdId(), savedItems.get(0).getProdId());
        assertEquals(item1.getQuantity(), savedItems.get(0).getQuantity());
        assertNotNull(savedItems.get(1).getOrder());
        assertEquals(orderId, savedItems.get(1).getOrder().getOrderId());
        assertEquals(item2.getProdId(), savedItems.get(1).getProdId());
        assertEquals(item2.getQuantity(), savedItems.get(1).getQuantity());

    }

    @Test
    @DisplayName("PlaceOrder: Throws OrderPlacementException if getCartItems returns empty list")
    void placeOrder_WhenGetCartItemsEmpty_ShouldThrowOrderPlacementException() {
        // Arrange
        when(cartServiceClient.getCartItemsByUserId(userId)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(OrderPlacementException.class, () -> {
            orderService.placeOrder(userId);
        });
        verify(cartServiceClient).getCartItemsByUserId(userId);
        verifyNoInteractions(orderRepository, orderitemsRepository); // Nothing should be saved
    }

    @Test
    @DisplayName("PlaceOrder: Throws OrderPlacementException if getCartItems returns null")
    void placeOrder_WhenGetCartItemsNull_ShouldThrowOrderPlacementException() {
        // Arrange
        when(cartServiceClient.getCartItemsByUserId(userId)).thenReturn(null);

        // Act & Assert
        assertThrows(OrderPlacementException.class, () -> {
            orderService.placeOrder(userId);
        });
        verify(cartServiceClient).getCartItemsByUserId(userId);
        verifyNoInteractions(orderRepository, orderitemsRepository);
    }

    @Test
    @DisplayName("PlaceOrder: Throws OrderPlacementException if getCartItems fails (FeignException)")
    void placeOrder_WhenGetCartItemsFails_ShouldThrowOrderPlacementException() {
        // Arrange
        when(cartServiceClient.getCartItemsByUserId(userId)).thenThrow(createFeignServerErrorException());

        // Act & Assert
        // Note: The service wraps generic FeignException in OrderPlacementException for this call
        assertThrows(OrderPlacementException.class, () -> {
            orderService.placeOrder(userId);
        });
        verify(cartServiceClient).getCartItemsByUserId(userId);
        verifyNoInteractions(orderRepository, orderitemsRepository);
    }

    @Test
    @DisplayName("PlaceOrder: Throws OrderPlacementException if getCartItems fails (NotFound)")
    void placeOrder_WhenGetCartItemsNotFound_ShouldThrowOrderPlacementException() {
        // Arrange
        when(cartServiceClient.getCartItemsByUserId(userId)).thenThrow(createFeignNotFoundException());

        // Act & Assert
        assertThrows(OrderPlacementException.class, () -> {
            orderService.placeOrder(userId);
        });
        verify(cartServiceClient).getCartItemsByUserId(userId);
        verifyNoInteractions(orderRepository, orderitemsRepository);
    }


    @Test
    @DisplayName("PlaceOrder: Throws ResourceNotFoundException if getCartId fails (NotFound)")
    void placeOrder_WhenGetCartIdNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(cartServiceClient.getCartItemsByUserId(userId)).thenReturn(sampleCartItems); // Items fetched OK
        when(cartServiceClient.getCartIdByUserId(userId)).thenThrow(createFeignNotFoundException()); // Cart ID fetch fails

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.placeOrder(userId);
        });
        verify(cartServiceClient).getCartItemsByUserId(userId);
        verify(cartServiceClient).getCartIdByUserId(userId);
        verifyNoInteractions(orderRepository, orderitemsRepository);
    }

    @Test
    @DisplayName("PlaceOrder: Throws OperationFailedException if save order fails")
    void placeOrder_WhenSaveOrderFails_ShouldThrowOperationFailedException() {
        // Arrange
        when(cartServiceClient.getCartItemsByUserId(userId)).thenReturn(sampleCartItems);
        when(cartServiceClient.getCartIdByUserId(userId)).thenReturn(cartId);
        when(orderRepository.save(any(Order.class))).thenThrow(new DataAccessException("DB Save Order Error") {});

        // Act & Assert
        assertThrows(OperationFailedException.class, () -> {
            orderService.placeOrder(userId);
        });
        verify(cartServiceClient).getCartItemsByUserId(userId);
        verify(cartServiceClient).getCartIdByUserId(userId);
        verify(orderRepository).save(any(Order.class));
        verifyNoInteractions(orderitemsRepository); // Items save not reached
    }

    @Test
    @DisplayName("PlaceOrder: Throws OperationFailedException if save items fails")
    void placeOrder_WhenSaveItemsFails_ShouldThrowOperationFailedException() {
        // Arrange
        Order expectedSavedOrder = new Order(orderId, userId, cartId, LocalDateTime.now(), 1350.0, new ArrayList<>());

        when(cartServiceClient.getCartItemsByUserId(userId)).thenReturn(sampleCartItems);
        when(cartServiceClient.getCartIdByUserId(userId)).thenReturn(cartId);
        when(orderRepository.save(any(Order.class))).thenReturn(expectedSavedOrder); // Order save succeeds
        when(orderitemsRepository.saveAll(any(List.class))).thenThrow(new DataAccessException("DB Save Items Error") {}); // Items save fails

        // Act & Assert
        assertThrows(OperationFailedException.class, () -> {
            orderService.placeOrder(userId);
        });
        verify(cartServiceClient).getCartItemsByUserId(userId);
        verify(cartServiceClient).getCartIdByUserId(userId);
        verify(orderRepository).save(any(Order.class));
        verify(orderitemsRepository).saveAll(any(List.class)); // Verify saveAll was attempted
    }

    @Test
    @DisplayName("PlaceOrder: Throws OperationFailedException if clearCart fails")
    void placeOrder_WhenClearCartFails_ShouldThrowOperationFailedException() {
        // Arrange - Everything succeeds until clearCart
        Order expectedSavedOrder = new Order(orderId, userId, cartId, LocalDateTime.now(), 1350.0, new ArrayList<>());
        when(cartServiceClient.getCartItemsByUserId(userId)).thenReturn(sampleCartItems);
        when(cartServiceClient.getCartIdByUserId(userId)).thenReturn(cartId);
        when(orderRepository.save(any(Order.class))).thenReturn(expectedSavedOrder);
        when(orderitemsRepository.saveAll(any(List.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        OperationFailedException ex = assertThrows(OperationFailedException.class, () -> {
            orderService.placeOrder(userId);
        });
        assertTrue(ex.getMessage().contains("failed to clear the cart")); // Check specific message

        // Verify all steps up to clearCart were called
        verify(cartServiceClient).getCartItemsByUserId(userId);
        verify(cartServiceClient).getCartIdByUserId(userId);
        verify(orderRepository).save(any(Order.class));
        verify(orderitemsRepository).saveAll(any(List.class));
    }


    // --- getOrdersByUserId Tests ---
    @Test
    @DisplayName("GetOrdersByUserId: Success returns list")
    void getOrdersByUserId_WhenFound_ReturnsList() {
        Order order1 = new Order(101, userId, 10, LocalDateTime.now(), 100.0, List.of());
        Order order2 = new Order(102, userId, 11, LocalDateTime.now(), 200.0, List.of());
        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of(order1, order2));

        List<Order> result = orderService.getOrdersByUserId(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderRepository).findAllByUserId(userId);
    }

    @Test
    @DisplayName("GetOrdersByUserId: Throws ResourceNotFoundException if none found")
    void getOrdersByUserId_WhenNoneFound_ThrowsResourceNotFoundException() {
        when(orderRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.getOrdersByUserId(userId);
        });
        verify(orderRepository).findAllByUserId(userId);
    }


    // --- getOrderByOrderId Tests ---
    @Test
    @DisplayName("GetOrderByOrderId: Success returns order")
    void getOrderByOrderId_WhenFound_ReturnsOrder() {
        Order order = new Order(orderId, userId, cartId, LocalDateTime.now(), 1350.0, List.of());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderService.getOrderByOrderId(orderId);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        verify(orderRepository).findById(orderId);
    }

    @Test
    @DisplayName("GetOrderByOrderId: Throws ResourceNotFoundException if not found")
    void getOrderByOrderId_WhenNotFound_ThrowsResourceNotFoundException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.getOrderByOrderId(orderId);
        });
        verify(orderRepository).findById(orderId);
    }

    // --- getAllOrders Tests ---
    @Test
    @DisplayName("GetAllOrders: Success returns list")
    void getAllOrders_WhenOrdersExist_ReturnsList() {
        Order order1 = new Order(101, 1, 10, LocalDateTime.now(), 100.0, List.of());
        Order order2 = new Order(102, 2, 11, LocalDateTime.now(), 200.0, List.of());
        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

        List<Order> result = orderService.getAllOrders();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderRepository).findAll();
    }

    @Test
    @DisplayName("GetAllOrders: Throws ResourceNotFoundException if none exist")
    void getAllOrders_WhenNoneExist_ThrowsResourceNotFoundException() {
        when(orderRepository.findAll()).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.getAllOrders();
        });
        verify(orderRepository).findAll();
    }


    // --- deleteOrder Tests ---
    

    @Test
    @DisplayName("DeleteOrder: Throws ResourceNotFoundException if order not found for user")
    void deleteOrder_WhenOrderNotFoundForUser_ThrowsResourceNotFoundException() {
        // Arrange
        when(orderRepository.findByUserIdAndOrderId(orderId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.deleteOrder(userId, orderId);
        });
        verify(orderRepository).findByUserIdAndOrderId(orderId, userId);
        verifyNoInteractions(orderitemsRepository);
        verify(orderRepository, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("DeleteOrder: Throws OperationFailedException on item delete error")
    void deleteOrder_WhenItemDeleteFails_ThrowsOperationFailedException() {
        // Arrange
        Order orderToDelete = new Order(orderId, userId, cartId, LocalDateTime.now(), 1350.0, List.of());
        when(orderRepository.findByUserIdAndOrderId(orderId, userId)).thenReturn(Optional.of(orderToDelete));
        doThrow(new OperationFailedException("DB Error") {}).when(orderitemsRepository).deleteAllByOrder(orderToDelete);

        // Act & Assert
        assertThrows(OperationFailedException.class, () -> {
            orderService.deleteOrder(userId, orderId);
        });
        verify(orderRepository).findByUserIdAndOrderId(orderId, userId);
        verify(orderitemsRepository).deleteAllByOrder(orderToDelete);
        verify(orderRepository, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("DeleteOrder: Throws OperationFailedException on order delete error")
    void deleteOrder_WhenOrderDeleteFails_ThrowsOperationFailedException() {
        // Arrange
        Order orderToDelete = new Order(orderId, userId, cartId, LocalDateTime.now(), 1350.0, List.of());
        when(orderRepository.findByUserIdAndOrderId(orderId, userId)).thenReturn(Optional.of(orderToDelete));

        doThrow(new DataAccessException("DB Error") {}).when(orderRepository).deleteById(orderId);

        // Act & Assert
        assertThrows(OperationFailedException.class, () -> {
            orderService.deleteOrder(userId, orderId);
        });
        verify(orderRepository).findByUserIdAndOrderId(orderId, userId);
        verify(orderitemsRepository).deleteAllByOrder(orderToDelete);
        verify(orderRepository).deleteById(orderId);
    }
}