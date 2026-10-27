package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Value("${bank.transfer.fee-enabled:false}")
    private boolean feeEnabled;

    @Value("${bank.transfer.fee-account:999999}")
    private String feeAccount;

    @Override
    public void run(String... args) {

        // Insert sample customers only when database is empty.
        if (customerRepository.count() == 0) {

            customerRepository.save(
                new Customer(
                    "100001",
                    "Kamal Silva",
                    "kamal@example.com",
                    new BigDecimal("50000.00")
                )
            );

            customerRepository.save(
                new Customer(
                    "100002",
                    "Nimali Perera",
                    "nimali@example.com",
                    new BigDecimal("25000.00")
                )
            );

            customerRepository.save(
                new Customer(
                    "100003",
                    "Saman Fernando",
                    "saman@example.com",
                    new BigDecimal("10000.00")
                )
            );
        }

        // Create bank fee account for extension 4,
        // only when the fee extension is enabled.
        if (feeEnabled &&
                customerRepository.countAccounts(feeAccount) == 0) {

            customerRepository.save(
                new Customer(
                    feeAccount,
                    "Bank Fee Account",
                    "bankfees@example.com",
                    BigDecimal.ZERO
                )
            );
        }
    }
}