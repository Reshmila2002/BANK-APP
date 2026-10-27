package com.example.bankapp.service;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    @Value("${bank.transfer.fee-enabled:false}")
    private boolean feeEnabled;

    @Value("${bank.transfer.fee-account:999999}")
    private String feeAccount;

    // --------------------------------
    // Customer operations
    // --------------------------------

    public List<Customer> getCustomers(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return customerRepository.findAllOrdered();
        }

        return customerRepository.searchByName(keyword.trim());
    }

    public Customer getCustomer(String accountNumber) {

        return customerRepository
            .getByAccountNumber(accountNumber)
            .orElseThrow(
                () -> new AccountNotFoundException(accountNumber)
            );
    }

    public long getCustomerCount() {
        return customerRepository.countCustomersNative();
    }

    public BigDecimal getTotalBalance() {

        BigDecimal total = customerRepository.getTotalBalance();

        return total == null ? BigDecimal.ZERO : total;
    }

    // --------------------------------
    // Transaction reporting
    // --------------------------------

    public List<BankTransaction> getLatestTransactions() {
        return transactionRepository.findAllLatest();
    }

    public long countTransactions(TransactionStatus status) {
        return transactionRepository.countByStatus(status);
    }

    public List<BankTransaction> getHistory(String account) {
        return transactionRepository.findHistory(account);
    }

    // Extension 1: Large Transfers
    public List<BankTransaction> getLargeTransfers(BigDecimal min) {

        if (min == null || min.signum() < 0) {
            throw new BankException(
                "Minimum amount cannot be negative."
            );
        }

        return transactionRepository.findLargeTransfers(
            min,
            TransactionStatus.SUCCESS
        );
    }

    // Extension 2: Total amount sent
    public BigDecimal getTotalSent(String account) {

        BigDecimal total = transactionRepository.getTotalSent(
            account,
            TransactionStatus.SUCCESS
        );

        return total == null ? BigDecimal.ZERO : total;
    }

    public boolean isFeeEnabled() {
        return feeEnabled;
    }

    // --------------------------------
    // Customer registration
    // --------------------------------

    @Transactional
    public Customer createCustomer(Customer customer) {

        if (customerRepository.countAccounts(
                customer.getAccountNumber()) > 0) {

            throw new BankException(
                "Account number already exists."
            );
        }

        if (customerRepository.countEmails(
                customer.getEmail()) > 0) {

            throw new BankException(
                "Email already exists."
            );
        }

        if (customer.getBalance() == null ||
                customer.getBalance().signum() < 0) {

            throw new BankException(
                "Opening balance cannot be negative."
            );
        }

        return customerRepository.save(customer);
    }

    // --------------------------------
    // Main Bank Transfer
    // --------------------------------

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(TransferRequest request) {

        if (request == null) {
            throw new BankException(
                "Transfer request is required."
            );
        }

        String from = request.getFromAccount();
        String to = request.getToAccount();
        BigDecimal amount = request.getAmount();

        // Step 1: Validate amount
        validateAmount(amount);

        // Step 2: Validate account numbers
        if (from == null || to == null ||
                from.isBlank() || to.isBlank()) {

            throw new BankException(
                "Both accounts are required."
            );
        }

        if (from.equals(to)) {
            throw new BankException(
                "Sender and receiver must be different accounts."
            );
        }

        // Verify both accounts exist
        getCustomer(from);
        getCustomer(to);

        // --------------------------------
        // Calculate optional bank fee
        // --------------------------------

        BigDecimal fee = BigDecimal.ZERO;

        if (feeEnabled) {

            if (from.equals(feeAccount) ||
                    to.equals(feeAccount)) {

                throw new BankException(
                    "Bank fee account cannot be used for transfers."
                );
            }

            // Verify bank fee account exists
            getCustomer(feeAccount);

            // Calculate 1% of transfer amount
            fee = amount
                .multiply(new BigDecimal("0.01"))
                .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalDebit = amount.add(fee);

        // --------------------------------
        // Debit sender
        // --------------------------------

        int debitRows = customerRepository.debit(
            from,
            totalDebit
        );

        if (debitRows != 1) {
            throw new InsufficientBalanceException();
        }

        // --------------------------------
        // Simulate system failure
        // --------------------------------

        if (request.isSimulateFailure()) {

            throw new BankException(
                "Simulated system failure. " +
                "All changes were rolled back."
            );
        }

        // --------------------------------
        // Credit receiver
        // --------------------------------

        int creditRows = customerRepository.credit(
            to,
            amount
        );

        if (creditRows != 1) {
            throw new BankException(
                "Receiver could not be credited."
            );
        }

        // --------------------------------
        // Credit bank fee
        // --------------------------------

        if (feeEnabled && fee.signum() > 0) {

            int feeRows = customerRepository.credit(
                feeAccount,
                fee
            );

            if (feeRows != 1) {
                throw new BankException(
                    "Unable to credit the bank fee."
                );
            }
        }

        // --------------------------------
        // Save SUCCESS transaction
        // --------------------------------

        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount(from);
        transaction.setToAccount(to);
        transaction.setAmount(amount);
        transaction.setFee(fee);
        transaction.setType("TRANSFER");
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(
            "Money transferred successfully."
        );

        return transactionRepository.save(transaction);
    }

    // --------------------------------
    // Extension 3: Deposit
    // --------------------------------

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction deposit(
            String account,
            BigDecimal amount) {

        validateAmount(amount);

        getCustomer(account);

        int updated = customerRepository.credit(
            account,
            amount
        );

        if (updated != 1) {
            throw new BankException(
                "Deposit failed."
            );
        }

        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount("CASH");
        transaction.setToAccount(account);
        transaction.setAmount(amount);
        transaction.setFee(BigDecimal.ZERO);
        transaction.setType("DEPOSIT");
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Cash deposit.");

        return transactionRepository.save(transaction);
    }

    // --------------------------------
    // Extension 3: Withdraw
    // --------------------------------

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction withdraw(
            String account,
            BigDecimal amount) {

        validateAmount(amount);

        getCustomer(account);

        int updated = customerRepository.debit(
            account,
            amount
        );

        if (updated != 1) {
            throw new InsufficientBalanceException();
        }

        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount(account);
        transaction.setToAccount("CASH");
        transaction.setAmount(amount);
        transaction.setFee(BigDecimal.ZERO);
        transaction.setType("WITHDRAW");
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Cash withdrawal.");

        return transactionRepository.save(transaction);
    }

    // --------------------------------
    // Validate money amount
    // --------------------------------

    private void validateAmount(BigDecimal amount) {

        if (amount == null || amount.signum() <= 0) {
            throw new BankException(
                "Amount must be greater than zero."
            );
        }

        if (amount.scale() > 2 ||
                amount.precision() - amount.scale() > 12) {

            throw new BankException(
                "Enter a valid amount with up to 2 decimals."
            );
        }
    }
}