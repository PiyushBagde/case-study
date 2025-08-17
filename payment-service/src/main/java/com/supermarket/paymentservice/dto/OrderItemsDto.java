package com.supermarket.paymentservice.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemsDto {
    private int productId;
    private String productName;
    private int quantity;
    private double price;

}
