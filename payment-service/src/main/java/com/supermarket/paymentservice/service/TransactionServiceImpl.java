package com.supermarket.paymentservice.service;

import com.supermarket.paymentservice.dto.OrderDto;
import com.supermarket.paymentservice.exception.OperationFailedException;
import com.supermarket.paymentservice.exception.ResourceNotFoundException;
import com.supermarket.paymentservice.feign.BillingServiceClient;
import com.supermarket.paymentservice.feign.CartServiceClient;
import com.supermarket.paymentservice.model.PaymentMode;
import com.supermarket.paymentservice.model.Transaction;
import com.supermarket.paymentservice.repository.TransactionRepository;
import feign.FeignException;
import jakarta.validation.constraints.Min;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BillingServiceClient billingServiceClient;

    @Autowired
    private CartServiceClient cartServiceClient;

    @Override
    public Transaction proceedTransaction(int orderId, PaymentMode paymentMode) {
        // Step 1: Get order info from billing-service using Feign client
        OrderDto order;
        try {
            order = billingServiceClient.getOrderByOrderId(orderId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Order not found with ID: " + orderId + " in Billing Service.", e);
        } catch (FeignException e) {
            throw new OperationFailedException("Failed to retrieve order details from Billing Service.", e);
        } catch (Exception e) {
            throw new OperationFailedException("An unexpected error occurred while fetching order details.", e);
        }

        if (order == null) {
            throw new ResourceNotFoundException("Order not found with ID: " + orderId + " (Billing service returned null)");
        }


        Transaction newTransaction = new Transaction();
        newTransaction.setOrderId(orderId);
        newTransaction.setUserId(order.getUserId());
        newTransaction.setRequiredAmount(order.getTotalBillPrice());
        newTransaction.setPaymentMode(paymentMode);
        newTransaction.setPaymentTime(LocalDateTime.now()); // initial time
        newTransaction.setPaymentStatus("Pending"); // initial status

        try {
            return transactionRepository.save(newTransaction);
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to save initial transaction record.", e);
        } catch (Exception e) {
            throw new OperationFailedException("An unexpected error occurred while saving the transaction record.", e);
        }
    }

    @Override
    public void clearCartAndUpdateInventory(Transaction transaction) {
        if ("Completed".equals(transaction.getPaymentStatus())) {
            try {
                cartServiceClient.clearCartAndReduceStockForUser(transaction.getUserId());
            } catch (Exception e) {
                throw new OperationFailedException("Failed to clear cart and update inventory.", e);
            }
        }
    }

    @Override
    public void verifyTransaction(Transaction transaction, double receivedAmount) {
        if (receivedAmount < 0) {
            throw new IllegalArgumentException("Received amount cannot be negative.");
        }

        transaction.setReceivedAmount(receivedAmount); // Recording what was actually received
        transaction.setTransactionTime(LocalDateTime.now());

        if (receivedAmount >= transaction.getRequiredAmount()) {
            transaction.setBalanceAmount(0.0);
            transaction.setPaymentStatus("Completed");
            System.out.println(transaction.getUserId());
        } else {
            transaction.setBalanceAmount(transaction.getRequiredAmount() - receivedAmount);
            transaction.setPaymentStatus("Incomplete");
        }
    }

    @Override
    @Transactional
    public Transaction payByCard(int orderId, double receivedAmount, String cardNumber, String cardHolderName) {
        if (cardNumber == null || cardNumber.isBlank() || cardHolderName == null || cardHolderName.isBlank()) {
            throw new IllegalArgumentException("Card number and holder name are required");
        }

        Transaction transaction = proceedTransaction(orderId, PaymentMode.CARD);

        transaction.setCardNumber(cardNumber);
        transaction.setCardHolderName(cardHolderName);
        verifyTransaction(transaction, receivedAmount);
        clearCartAndUpdateInventory(transaction);

        try {
            return transactionRepository.save(transaction);
        } catch (DataAccessException e) {
            // Transaction should roll back
            throw new OperationFailedException("Failed to save final card payment transaction details.", e);
        }
    }

    @Override
    @Transactional
    public Transaction payByUpi(int orderId, double receivedAmount, String upiId) {
        if (upiId == null || upiId.isBlank()) {
            throw new IllegalArgumentException("UPI ID is required for UPI payments");
        }
        Transaction transaction = proceedTransaction(orderId, PaymentMode.UPI);
        transaction.setUpiId(upiId);
        verifyTransaction(transaction, receivedAmount);
        clearCartAndUpdateInventory(transaction);

        try {
            return transactionRepository.save(transaction);
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to save final UPI payment transaction details.", e);
        }
    }

    @Override
    @Transactional
    public Transaction payByCash(int orderId, double receivedAmount) {
        Transaction transaction = proceedTransaction(orderId, PaymentMode.CASH);
        verifyTransaction(transaction, receivedAmount);
        clearCartAndUpdateInventory(transaction);
        try {
            return transactionRepository.save(transaction);
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to save final cash payment transaction details.", e);
        }
    }


    @Override
    public Transaction getPaymentById(int transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found with ID: " + transactionId));
    }

    @Override
    public List<Transaction> getPaymentsByMode(PaymentMode mode) {

        try {
            return transactionRepository.findAllByPaymentMode(mode);
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to retrieve payment transactions by mode.", e);
        }
    }

    @Override
    public List<Transaction> getAllPayments() {
        try {
            return transactionRepository.findAll();
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to retrieve all transactions.", e);
        }
    }

    @Override
    public List<Transaction> getAllPaymentsByUserId(int userId) {
        try {
            return transactionRepository.findAllByUserId(userId);
        } catch (DataAccessException e) {
            throw new OperationFailedException("Failed to retrieve transactions for user ID: " + userId, e);
        }
    }

    @Override
    public Transaction getMyTransactionById(int userId, int transactionId) {
        return transactionRepository.findByTransactionIdAndUserId(transactionId, userId).orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId + " for user ID: " + userId));
    }


    @Override
	public Transaction getMyTransactionByOrderId(int userId, int orderId) {
		if(userId<=0) {
			throw new IllegalArgumentException("User id cannot be 0 or negative");
		}
		if(orderId<=0) {
			throw new IllegalArgumentException("Order id cannot be 0 or negative");
		}
		Transaction transaction = transactionRepository.findByUserIdAndOrderId(userId, orderId).orElseThrow(() -> new ResourceNotFoundException("Transaction not found for User ID: "+userId+" and Order ID: "+ orderId));
		return transaction;
	}
}
















