package com.supermarket.paymentservice.service;

import com.supermarket.paymentservice.dto.OrderDto;
import com.supermarket.paymentservice.exception.OperationFailedException;
import com.supermarket.paymentservice.exception.ResourceNotFoundException;
import com.supermarket.paymentservice.feign.BillingServiceClient;
import com.supermarket.paymentservice.model.PaymentMode;
import com.supermarket.paymentservice.model.Transaction;
import com.supermarket.paymentservice.repository.TransactionRepository;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BillingServiceClient billingServiceClient;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private OrderDto sampleOrderDto;
    private Transaction sampleTransaction;
    private final int userId = 1;
    private final int orderId = 101;
    private final int transactionId = 501;
    private final double orderAmount = 250.50;

    @BeforeEach
    void setUp() {
        sampleOrderDto = new OrderDto(orderId, userId, 10, orderAmount, LocalDateTime.now().minusMinutes(5), Collections.emptyList());
        // Sample initial transaction state (before verification)
        sampleTransaction = new Transaction();
        sampleTransaction.setTransactionId(transactionId);
        sampleTransaction.setOrderId(orderId);
        sampleTransaction.setUserId(userId);
        sampleTransaction.setRequiredAmount(orderAmount);
        sampleTransaction.setPaymentMode(PaymentMode.CASH); // Will be overridden in tests
        sampleTransaction.setPaymentStatus("Pending");
        sampleTransaction.setPaymentTime(LocalDateTime.now().minusMinutes(1)); // Set distinct time
    }

    // Helper to create a FeignException.NotFound
    private FeignException.NotFound createFeignNotFoundException() {
        Request request = Request.create(Request.HttpMethod.GET, "/dummy", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.NotFound("Not Found", request, null, Collections.emptyMap());
    }
    // Helper to create a generic FeignException (e.g., 500 error)
    private FeignException createFeignServerErrorException() {
        Request request = Request.create(Request.HttpMethod.GET, "/dummy", Collections.emptyMap(), null, new RequestTemplate());
        return new FeignException.InternalServerError("Server Error", request, null, Collections.emptyMap());
    }

    @Test
    @DisplayName("ProceedTransaction: Success gets order, saves pending transaction")
    void proceedTransaction_Success() {
        // Arrange
        PaymentMode mode = PaymentMode.CARD;
        // Mock getting order
        when(billingServiceClient.getOrderByOrderId(orderId)).thenReturn(sampleOrderDto);
        // Mock saving the initial transaction
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setTransactionId(transactionId); // Simulate ID assignment
            return t;
        });

        // Act
        Transaction result = transactionService.proceedTransaction(orderId, mode);

        // Assert
        assertNotNull(result);
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(orderId, result.getOrderId());
        assertEquals(userId, result.getUserId());
        assertEquals(orderAmount, result.getRequiredAmount());
        assertEquals(mode, result.getPaymentMode());
        assertEquals("Pending", result.getPaymentStatus());
        assertNotNull(result.getPaymentTime());

        verify(billingServiceClient).getOrderByOrderId(orderId);
        verify(transactionRepository).save(transactionCaptor.capture());
        // Check state before save
        assertEquals(orderId, transactionCaptor.getValue().getOrderId());
        assertEquals("Pending", transactionCaptor.getValue().getPaymentStatus());
    }

    @Test
    @DisplayName("ProceedTransaction: Throws ResourceNotFoundException if order returns null (edge case)")
    void proceedTransaction_WhenOrderNull_ThrowsResourceNotFoundException() {
        // Arrange
        when(billingServiceClient.getOrderByOrderId(orderId)).thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            transactionService.proceedTransaction(orderId, PaymentMode.CASH);
        });
        verify(billingServiceClient).getOrderByOrderId(orderId);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    @DisplayName("ProceedTransaction: Throws ResourceNotFoundException on Feign NotFound")
    void proceedTransaction_WhenFeignNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(billingServiceClient.getOrderByOrderId(orderId)).thenThrow(createFeignNotFoundException());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            transactionService.proceedTransaction(orderId, PaymentMode.CASH);
        });
        verify(billingServiceClient).getOrderByOrderId(orderId);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    @DisplayName("ProceedTransaction: Throws OperationFailedException on other Feign Error")
    void proceedTransaction_WhenFeignError_ThrowsOperationFailedException() {
        // Arrange
        when(billingServiceClient.getOrderByOrderId(orderId)).thenThrow(createFeignServerErrorException());

        // Act & Assert
        assertThrows(OperationFailedException.class, () -> {
            transactionService.proceedTransaction(orderId, PaymentMode.CASH);
        });
        verify(billingServiceClient).getOrderByOrderId(orderId);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    @DisplayName("ProceedTransaction: Throws OperationFailedException on DB save error")
    void proceedTransaction_WhenSaveFails_ThrowsOperationFailedException() {
        // Arrange
        when(billingServiceClient.getOrderByOrderId(orderId)).thenReturn(sampleOrderDto);
        when(transactionRepository.save(any(Transaction.class))).thenThrow(new DataAccessException("DB Error") {});

        // Act & Assert
        assertThrows(OperationFailedException.class, () -> {
            transactionService.proceedTransaction(orderId, PaymentMode.CASH);
        });
        verify(billingServiceClient).getOrderByOrderId(orderId);
        verify(transactionRepository).save(any(Transaction.class));
    }


    // --- verifyTransaction Tests ---

    @Test
    @DisplayName("VerifyTransaction: Marks Completed when amount >= required")
    void verifyTransaction_SufficientAmount_MarksCompleted() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setRequiredAmount(100.0);
        tx.setPaymentStatus("Pending"); // Initial state
        double receivedAmount = 100.0;

        // Act
        transactionService.verifyTransaction(tx, receivedAmount);

        // Assert
        assertEquals(receivedAmount, tx.getReceivedAmount());
        assertEquals(0.0, tx.getBalanceAmount());
        assertEquals("Completed", tx.getPaymentStatus());
        assertNotNull(tx.getTransactionTime()); // Time should be set
    }

    @Test
    @DisplayName("VerifyTransaction: Marks Completed when amount > required")
    void verifyTransaction_OverpaidAmount_MarksCompleted() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setRequiredAmount(100.0);
        tx.setPaymentStatus("Pending");
        double receivedAmount = 110.0; // Overpaid

        // Act
        transactionService.verifyTransaction(tx, receivedAmount);

        // Assert
        assertEquals(receivedAmount, tx.getReceivedAmount());
        assertEquals(0.0, tx.getBalanceAmount()); // Balance is still 0
        assertEquals("Completed", tx.getPaymentStatus());
        assertNotNull(tx.getTransactionTime());
    }


    @Test
    @DisplayName("VerifyTransaction: Marks Incomplete when amount < required")
    void verifyTransaction_InsufficientAmount_MarksIncomplete() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setRequiredAmount(100.0);
        tx.setPaymentStatus("Pending");
        double receivedAmount = 80.0;
        double expectedBalance = 20.0;

        // Act
        transactionService.verifyTransaction(tx, receivedAmount);

        // Assert
        assertEquals(receivedAmount, tx.getReceivedAmount());
        assertEquals(expectedBalance, tx.getBalanceAmount());
        assertEquals("Incomplete", tx.getPaymentStatus());
        assertNotNull(tx.getTransactionTime());
    }

    
    @Test
    @DisplayName("PayByCard: Throws IllegalArgumentException for blank card info")
    void payByCard_BlankCardInfo_ThrowsIllegalArgumentException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            transactionService.payByCard(orderId, orderAmount, "  ", "Test");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            transactionService.payByCard(orderId, orderAmount, "1234", "  ");
        });
        verifyNoInteractions(billingServiceClient, transactionRepository); // Should fail before calling service/repo
    }


    // --- payByUpi Tests ---
    @Test
    @DisplayName("PayByUpi: Success processes payment")
    void payByUpi_ValidInput_ProcessesAndSaves() {
        // Arrange
        double receivedAmount = orderAmount - 10; // Incomplete amount
        String upiId = "test@okbank";

        when(billingServiceClient.getOrderByOrderId(orderId)).thenReturn(sampleOrderDto);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if ("Pending".equals(t.getPaymentStatus()) && t.getTransactionId() == 0) t.setTransactionId(transactionId);
            return t;
        });

        // Act
        Transaction result = transactionService.payByUpi(orderId, receivedAmount, upiId);

        // Assert
        assertNotNull(result);
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(PaymentMode.UPI, result.getPaymentMode());
        assertEquals(upiId, result.getUpiId());
        assertEquals("Incomplete", result.getPaymentStatus());
        assertEquals(receivedAmount, result.getReceivedAmount());
        assertEquals(10.0, result.getBalanceAmount()); // Check balance
        assertNotNull(result.getTransactionTime());

        verify(billingServiceClient).getOrderByOrderId(orderId);
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        assertEquals("Incomplete", transactionCaptor.getValue().getPaymentStatus());
        assertEquals(upiId, transactionCaptor.getValue().getUpiId());
    }

    @Test
    @DisplayName("PayByUpi: Throws IllegalArgumentException for blank UPI ID")
    void payByUpi_BlankUpiId_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            transactionService.payByUpi(orderId, orderAmount, " ");
        });
        verifyNoInteractions(billingServiceClient, transactionRepository);
    }

    // --- payByCash Tests ---
    
    // --- getPaymentById Tests ---
    @Test
    @DisplayName("GetPaymentById: Success returns transaction")
    void getPaymentById_WhenFound_ReturnsTransaction() {
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(sampleTransaction));
        Transaction result = transactionService.getPaymentById(transactionId);
        assertNotNull(result);
        assertEquals(transactionId, result.getTransactionId());
        verify(transactionRepository).findById(transactionId);
    }

    @Test
    @DisplayName("GetPaymentById: Throws ResourceNotFoundException if not found")
    void getPaymentById_WhenNotFound_ThrowsResourceNotFoundException() {
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            transactionService.getPaymentById(transactionId);
        });
        verify(transactionRepository).findById(transactionId);
    }

    // --- getPaymentsByMode Tests ---
    // Assume repository method exists: findAllByPaymentMode
    @Test
    @DisplayName("GetPaymentsByMode: Success returns list")
    void getPaymentsByMode_WhenFound_ReturnsList() {
        sampleTransaction.setPaymentMode(PaymentMode.CARD);
        when(transactionRepository.findAllByPaymentMode(PaymentMode.CARD)).thenReturn(List.of(sampleTransaction));
        List<Transaction> result = transactionService.getPaymentsByMode(PaymentMode.CARD);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(PaymentMode.CARD, result.get(0).getPaymentMode());
        verify(transactionRepository).findAllByPaymentMode(PaymentMode.CARD);
    }

    // --- getAllPayments Tests ---
    @Test
    @DisplayName("GetAllPayments: Success returns all transactions")
    void getAllPayments_WhenFound_ReturnsList() {
        when(transactionRepository.findAll()).thenReturn(List.of(sampleTransaction));
        List<Transaction> result = transactionService.getAllPayments();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(transactionId, result.get(0).getTransactionId());
        verify(transactionRepository).findAll();
    }

    @Test
    @DisplayName("GetAllPayments: Throws OperationFailedException on DataAccessException")
    void getAllPayments_WhenDataAccessFails_ThrowsOperationFailedException() {
        when(transactionRepository.findAll()).thenThrow(new DataAccessException("DB Error") {
        });
        assertThrows(OperationFailedException.class, () -> {
            transactionService.getAllPayments();
        });
        verify(transactionRepository).findAll();
    }

    // --- getAllPaymentsByUserId Tests ---
    @Test
    @DisplayName("GetAllPaymentsByUserId: Success returns user transactions")
    void getAllPaymentsByUserId_WhenFound_ReturnsList() {
        when(transactionRepository.findAllByUserId(userId)).thenReturn(List.of(sampleTransaction));
        List<Transaction> result = transactionService.getAllPaymentsByUserId(userId);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(userId, result.get(0).getUserId());
        verify(transactionRepository).findAllByUserId(userId);
    }

    @Test
    @DisplayName("GetAllPaymentsByUserId: Throws OperationFailedException on DataAccessException")
    void getAllPaymentsByUserId_WhenDataAccessFails_ThrowsOperationFailedException() {
        when(transactionRepository.findAllByUserId(userId)).thenThrow(new DataAccessException("DB Error") {
        });
        assertThrows(OperationFailedException.class, () -> {
            transactionService.getAllPaymentsByUserId(userId);
        });
        verify(transactionRepository).findAllByUserId(userId);
    }

    // --- getMyTransactionById Tests ---
    @Test
    @DisplayName("GetMyTransactionById: Success returns specific user transaction")
    void getMyTransactionById_WhenFound_ReturnsTransaction() {
        when(transactionRepository.findByTransactionIdAndUserId(transactionId, userId)).thenReturn(Optional.of(sampleTransaction));
        Transaction result = transactionService.getMyTransactionById(userId, transactionId);
        assertNotNull(result);
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(userId, result.getUserId());
        verify(transactionRepository).findByTransactionIdAndUserId(transactionId, userId);
    }

    @Test
    @DisplayName("GetMyTransactionById: Throws ResourceNotFoundException if not found")
    void getMyTransactionById_WhenNotFound_ThrowsResourceNotFoundException() {
        when(transactionRepository.findByTransactionIdAndUserId(transactionId, userId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            transactionService.getMyTransactionById(userId, transactionId);
        });
        verify(transactionRepository).findByTransactionIdAndUserId(transactionId, userId);
    }

}