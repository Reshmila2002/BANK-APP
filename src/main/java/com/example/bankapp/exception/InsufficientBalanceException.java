package com.example.bankapp.exception;

public class InsufficientBalanceException extends BankException {

    public InsufficientBalanceException() {
        super("Insufficient balance to complete the transaction.");
    }
}