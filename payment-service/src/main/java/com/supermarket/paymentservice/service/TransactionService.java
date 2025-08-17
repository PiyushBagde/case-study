package com.supermarket.paymentservice.service;

import java.util.List;

import com.supermarket.paymentservice.model.PaymentMode;
import com.supermarket.paymentservice.model.Transaction;

public interface TransactionService {
	Transaction proceedTransaction(int orderId, PaymentMode paymentMode);
	void clearCartAndUpdateInventory(Transaction transaction);
	void verifyTransaction(Transaction transaction, double receivedAmount);
	Transaction payByCard(int orderId, double receivedAmount, String cardNumber, String cardHolderName);
	Transaction payByUpi(int orderId, double receivedAmount, String upiId);
	Transaction payByCash(int orderId, double receivedAmount);
	Transaction getPaymentById(int transactionId);
	List<Transaction> getPaymentsByMode(PaymentMode mode);
	List<Transaction> getAllPayments();
	List<Transaction> getAllPaymentsByUserId(int userId);
	Transaction getMyTransactionById(int userId, int transactionId);
	Transaction getMyTransactionByOrderId(int userId, int orderId);
}
