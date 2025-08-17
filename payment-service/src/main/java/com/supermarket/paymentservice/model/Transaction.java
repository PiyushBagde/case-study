package com.supermarket.paymentservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "payment")
public class Transaction {
    @Id
    @Column(name = "transaction_id") //modify to 32 bit
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int transactionId;

    @Column(name = "user_id")
    @Min(value = 1, message = "User ID must be positive")
    private int userId;

    @Column(name = "order_id")
    @Min(value = 1, message = "Order ID must be positive")
    private int orderId;

    @Column(name = "required_amount")
    @PositiveOrZero(message = "Required amount cannot be negative")
    private double requiredAmount;

    @Column(name = "received_amount")
    @PositiveOrZero(message = "Required amount cannot be negative")
    private double receivedAmount;

    @Column(name = "balance_amount")
    private double balanceAmount;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode; //Cash, Card, UPI

    @Column(name = "payment_status")
    @NotBlank(message = "Status connot be blank")
    private String paymentStatus;

    @Column(name = "payment_time")
    @NotNull(message = "Payment mode cannot be null")
    private LocalDateTime paymentTime;

    private String upiId; // For UPI
    @PastOrPresent(message = "Payment time cannot be in the future")
    private LocalDateTime transactionTime;

    @Column(name = "card_number")
    private String cardNumber;

    @Column(name = "card_holder_name")// For Card
    private String cardHolderName;

}
