package com.supermarket.billingservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private int orderId;

    @Column(name = "user_id")
    @Min(value = 1, message = "User ID must be positive")
    private int userId;

    @Column(name = "cart_id")
    @Min(value = 1, message = "Cart ID must be positive")
    private int cartId;

    @Column(name = "order_date")
    @PastOrPresent(message = "Order date cannot be in the future")
    private LocalDateTime orderDate;

    @Column(name = "total_price")
    @PositiveOrZero(message = "Total bill price cannot be negative")
    private double totalBillPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Valid
    private List<OrderItems> orderItems = new ArrayList<>();

}
