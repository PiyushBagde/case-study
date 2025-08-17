package com.supermarket.paymentservice.repository;

import com.supermarket.paymentservice.model.PaymentMode;
import com.supermarket.paymentservice.model.Transaction;

import jakarta.validation.constraints.Min;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    List<Transaction> findAllByUserId(int userId);

    Optional<Transaction> findByTransactionIdAndUserId(int transactionId, int userId);

    List<Transaction> findAllByPaymentMode(PaymentMode paymentMode);

	Optional<Transaction> findByUserIdAndOrderId(int userId,int orderId);
}
