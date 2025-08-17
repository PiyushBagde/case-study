package com.supermarket.billingservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "order_items")
public class OrderItems {

    @Id
    @Column(name = "order_item_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderItemId;

    @Column(name = "product_id")
    @Positive(message = "Product ID must be positive")
    private int prodId;

    @Column(name = "product_name")
    @NotBlank(message = "Product name cannot be blank")
    private String prodName;

    @Column(name = "price")
    @PositiveOrZero(message = "Price cannot be negative")
    private double price;

    @Column(name = "quantity")
    @Positive(message = "Quantity must be positive")
    private int quantity;

    @Column(name = "total_price")
    @PositiveOrZero(message = "Total price cannot be negative")
    private double totalPrice;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    @NotNull
    private Order order;

}
