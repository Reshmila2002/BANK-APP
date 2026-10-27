package com.example.bankapp.repository;

import com.example.bankapp.model.Customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    // 1. Find customer by account number
    @Query("""
        SELECT c FROM Customer c
        WHERE c.accountNumber = :accountNumber
        """)
    Optional<Customer> getByAccountNumber(
        @Param("accountNumber") String accountNumber
    );

    // 2. Check duplicate account number
    @Query("""
        SELECT COUNT(c) FROM Customer c
        WHERE c.accountNumber = :accountNumber
        """)
    long countAccounts(
        @Param("accountNumber") String accountNumber
    );

    // 3. Check duplicate email
    @Query("""
        SELECT COUNT(c) FROM Customer c
        WHERE LOWER(c.email) = LOWER(:email)
        """)
    long countEmails(
        @Param("email") String email
    );

    // 4. Get all customers sorted by name
    @Query("""
        SELECT c FROM Customer c
        ORDER BY c.fullName ASC
        """)
    List<Customer> findAllOrdered();

    // 5. Search customers by name
    @Query("""
        SELECT c FROM Customer c
        WHERE LOWER(c.fullName)
        LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY c.fullName ASC
        """)
    List<Customer> searchByName(
        @Param("keyword") String keyword
    );

    // 6. Calculate total money
    @Query("""
        SELECT COALESCE(SUM(c.balance), 0)
        FROM Customer c
        """)
    BigDecimal getTotalBalance();

    // 7. Native SQL query
    @Query(
        value = "SELECT COUNT(*) FROM customers",
        nativeQuery = true
    )
    long countCustomersNative();

    // 8. Debit money safely
    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true
    )
    @Transactional
    @Query("""
        UPDATE Customer c
        SET c.balance = c.balance - :amount
        WHERE c.accountNumber = :accountNumber
        AND c.balance >= :amount
        """)
    int debit(
        @Param("accountNumber") String accountNumber,
        @Param("amount") BigDecimal amount
    );

    // 9. Credit money
    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true
    )
    @Transactional
    @Query("""
        UPDATE Customer c
        SET c.balance = c.balance + :amount
        WHERE c.accountNumber = :accountNumber
        """)
    int credit(
        @Param("accountNumber") String accountNumber,
        @Param("amount") BigDecimal amount
    );
}