package com.supermarket.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {
    private int orderId;
    private int userId;
    private int cartId;
    private double totalBillPrice;
    private LocalDateTime orderDate;
    private List<OrderItemsDto> items;
}
