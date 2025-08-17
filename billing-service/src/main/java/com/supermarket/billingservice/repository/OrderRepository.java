package com.supermarket.billingservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.supermarket.billingservice.model.Order;

public interface OrderRepository extends JpaRepository<Order, Integer>{

	List<Order> findByUserId(int userId);

	List<Order> findAllByUserId(int userId);

    Optional<Order> findByUserIdAndOrderId(int userId, int orderId);

}
