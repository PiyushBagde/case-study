package com.supermarket.paymentservice.controller;

import com.supermarket.paymentservice.model.PaymentMode;
import com.supermarket.paymentservice.model.Transaction;
import com.supermarket.paymentservice.service.TransactionServiceImpl;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.CreditCardNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/payment")
@Validated
public class TransactionController {

    @Autowired
    private TransactionServiceImpl transactionService;

    @PostMapping("/biller-customer/payByCard")
    public Transaction payByCard(
            @RequestParam @Min(value = 1, message = "Order ID must be positive") int orderId,
            @RequestParam @PositiveOrZero(message = "Received amount cannot be negative") double receivedAmount,
            @RequestParam @NotBlank(message = "Card number cannot be blank")
            @CreditCardNumber(message = "Invalid credit card number format")
            @Size(min = 13, max = 19, message = "Card number length seems invalid")
             String cardNumber,
            @RequestParam @NotBlank(message = "Card holder name cannot be blank")
            @Size(min = 2, max = 100, message = "Card holder name must be between 2 and 100 characters")
            String cardHolderName
    ) {
        return transactionService.payByCard(orderId, receivedAmount, cardNumber, cardHolderName);
    }

    @PostMapping("/biller-customer/payByUpi")
    public Transaction payByUpi(
            @RequestParam @Min(value = 1, message = "Order ID must be positive") int orderId,
            @RequestParam @PositiveOrZero(message = "Received amount cannot be negative") double receivedAmount,
            // Basic check: not blank. A pattern could be added for basic format check (e.g., xxx@yyy)
            @RequestParam @NotBlank(message = "UPI ID cannot be blank")
            // Example pattern (very basic, adjust as needed): user@bank
            @Pattern(regexp = "^[\\w.-]+@[\\w.-]+$", message = "Invalid UPI ID format")
            String upiId
    ) {
        return transactionService.payByUpi(orderId, receivedAmount, upiId);
    }

    @PostMapping("/biller-customer/payByCash")
    public Transaction payByCash(
            @RequestParam @Min(value = 1, message = "Order ID must be positive") int orderId,
            @RequestParam @PositiveOrZero(message = "Received amount cannot be negative") double receivedAmount
    ) {
        return transactionService.payByCash(orderId, receivedAmount);
    }

    @GetMapping("/admin/getPaymentById/{transactionId}")
    public Transaction getPaymentById(@PathVariable @Min(value = 1, message = "Transaction ID must be positive") int transactionId) {
        return transactionService.getPaymentById(transactionId);
    }

    @GetMapping("/admin/getPaymentByMode/{mode}")
    public List<Transaction> getPaymentsByMode( @PathVariable @NotNull(message = "Payment mode cannot be null") PaymentMode mode) {
        return transactionService.getPaymentsByMode(mode);
    }

    @GetMapping("/admin/getAllPayments")
    public List<Transaction> getAllPayments() {
        return transactionService.getAllPayments();

    }

    // complete get my transactions
    @GetMapping("/customer/getMyTransactions")
    public List<Transaction> getMyTransactions(@RequestHeader("X-UserId") int userId) {
        return transactionService.getAllPaymentsByUserId(userId);
    }

    @GetMapping("/customer/getMyTransactionById/{transactionId}")
    public Transaction getMyTransactionById(
            @RequestHeader("X-UserId") int userId,
            @PathVariable @Min(value = 1, message = "Transaction ID must be positive") int transactionId) {
        return transactionService.getMyTransactionById(userId, transactionId);
    }
    
    @GetMapping("/customer/getMyTransactionByOrderId/{orderId}")
    public Transaction getMyTransactionByOrderId(
            @RequestHeader("X-UserId") int userId,
            @PathVariable @Min(value = 1, message = "Order ID must be positive") int orderId) {
        return transactionService.getMyTransactionByOrderId(userId, orderId);
    }
    
    
    
    
}