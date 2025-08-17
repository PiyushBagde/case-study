package com.supermarket.billingservice.service;

import java.util.List;

import com.supermarket.billingservice.model.Order;

public interface OrderService {
	Order placeOrder(int userId);
	List<Order> getOrdersByUserId(int userId);
	Order getOrderByOrderId(int orderId);
	List<Order> getAllOrders();
	void deleteOrder(int userId, int orderId);
	Order getOrderByUserIdAndOrderId(int userId, int orderId);

}
