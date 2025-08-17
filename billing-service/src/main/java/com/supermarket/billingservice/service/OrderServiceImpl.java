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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService{
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderitemsRepository orderitemsRepository;

    @Autowired
    private CartServiceClient cartServiceClient;


    @Override
    @Transactional
    public Order placeOrder(int userId) {
        List<CartItemResponse> cartItems;
        try {
            cartItems = cartServiceClient.getCartItemsByUserId(userId);
        } catch (FeignException.NotFound e) {
            throw new OrderPlacementException("Cannot place order: Cart not found or is empty for user ID: " + userId, e);
        } catch (FeignException e) {
            throw new OrderPlacementException("Cannot place order: Error while fetching cart items.", e);
        }


        if (cartItems == null || cartItems.isEmpty()) {
            throw new OrderPlacementException("Cannot place order: Cart is empty for user ID: " + userId);
        }

        // check if cart exist for userId
        int cartId;
        try {
            cartId = cartServiceClient.getCartIdByUserId(userId);
        } catch (FeignException.NotFound e) {
            // Check in the cart service if items exist but cart ID doesn't
            throw new ResourceNotFoundException("Cannot place order: Associated Cart ID not found for user ID: " + userId, e);
        } catch (FeignException e) {
            throw new OperationFailedException("Failed to retrieve associated Cart ID from Cart Service.", e);
        } catch (Exception e) {
            throw new OperationFailedException("An unexpected error occurred while fetching the Cart ID.", e);
        }

        double totalAmount = 0.0;

        List<OrderItems> orderItemList = new ArrayList<>();

        for (CartItemResponse cartItem : cartItems) {
            if (cartItem.getQuantity() <= 0) continue;

            OrderItems item = new OrderItems();
            item.setProdId(cartItem.getProdId());
            item.setProdName(cartItem.getProdName());
            item.setQuantity(cartItem.getQuantity());
            item.setPrice(cartItem.getPrice());
            item.setTotalPrice(cartItem.getQuantity() * cartItem.getPrice());
            totalAmount += item.getTotalPrice();

            orderItemList.add(item);
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setOrderDate(LocalDateTime.now());
        order.setCartId(cartId); // cartId associated with the Order
        order.setTotalBillPrice(totalAmount);

        Order savedOrder;
        try {
            savedOrder = orderRepository.save(order);
            for (OrderItems item : orderItemList) {
                item.setOrder(savedOrder);
            }
            orderitemsRepository.saveAll(orderItemList); // saving all items
            savedOrder.setOrderItems(orderItemList);
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to save order details to database.", e);
        } catch (Exception e) {
            throw new OperationFailedException("An unexpected error occurred while saving the order.", e);
        }
        return savedOrder;
    }

    @Override
    public List<Order> getOrdersByUserId(int userId) {
        try {
            List<Order> orders = orderRepository.findAllByUserId(userId);
            if (orders.isEmpty()) {
                throw new ResourceNotFoundException("No orders found for user ID: " + userId);
            }
            return orders;
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to retrieve orders for user ID: " + userId, e);
        }
    }

    @Override
    public Order getOrderByOrderId(int orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
    }

    @Override
    public List<Order> getAllOrders() {
        try {
            List<Order> orders = orderRepository.findAll();
            if (orders.isEmpty()) {
                throw new ResourceNotFoundException("No orders found.");
            }
            return orders;
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to retrieve all orders.", e);
        }
    }

    @Override
    @Transactional
    public void deleteOrder(int userId, int orderId) {
        Order order = orderRepository.findByUserIdAndOrderId(userId, orderId).orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found for user with id " + userId + "."));
        try {
            orderitemsRepository.deleteAllByOrder(order);
            orderRepository.deleteById(orderId);
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to delete order with id " + orderId + ".", e);
        } catch (Exception e) {
            throw new OperationFailedException("An unexpected error occurred while deleting the order with id " + orderId + ".", e);
        }
    }
   
    @Override
    public Order getOrderByUserIdAndOrderId(int userId, int orderId) {
    	Order order = orderRepository.findByUserIdAndOrderId(userId, orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found with UserId: "+ userId+ " and orderId: "+ orderId));
    	return order;
    }
}
