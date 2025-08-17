package com.supermarket.billingservice.repository;

import com.supermarket.billingservice.model.Order;


import org.springframework.data.jpa.repository.JpaRepository;

import com.supermarket.billingservice.model.OrderItems;

public interface OrderitemsRepository extends JpaRepository<OrderItems, Integer> {

    void deleteAllByOrder(Order order);

}
