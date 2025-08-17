package com.supermarket.billingservice.controller;

import java.util.List;

import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.supermarket.billingservice.model.Order;
import com.supermarket.billingservice.service.OrderServiceImpl;

@RestController
@RequestMapping("/bill")
@Validated
public class OrderController {
	@Autowired
	private OrderServiceImpl orderService;

	@PostMapping("/biller/placeOrder/{userId}")
	public Order placeOrder(@PathVariable @Min(value = 1, message = "User ID must be positive") int userId) {
		return orderService.placeOrder(userId);
	}

	@PostMapping("/customer/placeMyOrder")
	public Order placeMyOrder(@RequestHeader("X-UserId") int userId) {
		return orderService.placeOrder(userId);
	}

	@GetMapping("/customer/getMyOrders")
	public List<Order> getMyOrders(@RequestHeader("X-UserId") int userId) {
		return orderService.getOrdersByUserId(userId);
	}
	
	
	
	@GetMapping("/customer/getMyOrderById/{orderId}")
	public Order getMyOrderById(@RequestHeader("X-UserId") int userId, @PathVariable @Min(value = 1, message = "Order ID must be positive") int orderId) {
		return orderService.getOrderByUserIdAndOrderId(userId, orderId);
	}

	@DeleteMapping("/customer/cancelMyOrder/{orderId}")
	public String cancelMyOrder(
			@RequestHeader("X-UserId") int userId ,
			@PathVariable @Min(value = 1, message = "Order ID must be positive") int orderId) {
		orderService.deleteOrder(userId, orderId);
		return "Order Cancelled Successfully";
	}

	@DeleteMapping("/biller/cancelOrder/{userId}/{orderId}")
	public String cancelOrder(
			@PathVariable @Min(value = 1, message = "User ID must be positive") int userId,
			@PathVariable @Min(value = 1, message = "Order ID must be positive") int orderId
	) {
		orderService.deleteOrder(userId, orderId);
		return "Order Cancelled Successfully for user id: "+userId;
	}

	@GetMapping("/admin/getOrderByUserId/{userId}")
	public List<Order> getOrdersByUserId(@PathVariable @Min(value = 1, message = "User ID must be positive") int userId){
		return orderService.getOrdersByUserId(userId);
	}


	@GetMapping("/admin-biller/getOrderByOrderId/{orderId}")
	public Order getOrderByOrderId(@PathVariable @Min(value = 1, message = "Order ID must be positive") int orderId) {
		return orderService.getOrderByOrderId(orderId);
	}
	
	@GetMapping("/admin/getAllOrders")
	public List<Order> getAllOrders(){
		return orderService.getAllOrders();
	}
}

