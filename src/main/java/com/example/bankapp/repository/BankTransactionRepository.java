package com.example.bankapp.repository;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    // 1. Get all transactions, newest first
    @Query("""
        SELECT t FROM BankTransaction t
        ORDER BY t.createdAt DESC, t.id DESC
        """)
    List<BankTransaction> findAllLatest();

    // 2. Transaction history of a specific account
    @Query("""
        SELECT t FROM BankTransaction t
        WHERE t.fromAccount = :account
           OR t.toAccount = :account
        ORDER BY t.createdAt DESC, t.id DESC
        """)
    List<BankTransaction> findHistory(
        @Param("account") String account
    );

    // 3. Find transactions by SUCCESS or FAILED status
    @Query("""
        SELECT t FROM BankTransaction t
        WHERE t.status = :status
        ORDER BY t.createdAt DESC, t.id DESC
        """)
    List<BankTransaction> findByStatusLatest(
        @Param("status") TransactionStatus status
    );

    // 4. Count transactions by status
    @Query("""
        SELECT COUNT(t)
        FROM BankTransaction t
        WHERE t.status = :status
        """)
    long countByStatus(
        @Param("status") TransactionStatus status
    );

    // 5. Extension: Successful transfers ABOVE minimum amount
    @Query("""
        SELECT t FROM BankTransaction t
        WHERE t.type = 'TRANSFER'
        AND t.status = :status
        AND t.amount > :min
        ORDER BY t.createdAt DESC, t.id DESC
        """)
    List<BankTransaction> findLargeTransfers(
        @Param("min") BigDecimal min,
        @Param("status") TransactionStatus status
    );

    // 6. Extension: Total amount successfully sent by account
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM BankTransaction t
        WHERE t.fromAccount = :account
        AND t.status = :status
        AND t.type = 'TRANSFER'
        """)
    BigDecimal getTotalSent(
        @Param("account") String account,
        @Param("status") TransactionStatus status
    );
}