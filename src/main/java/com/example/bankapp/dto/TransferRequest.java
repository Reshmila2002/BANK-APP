package com.example.bankapp.dto;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TransferRequest {

    @NotBlank(message = "Sender account is required")
    private String fromAccount;

    @NotBlank(message = "Receiver account is required")
    private String toAccount;

    @NotNull(message = "Amount is required")
    @DecimalMin(
        value = "0.01",
        message = "Amount must be greater than zero"
    )
    @Digits(
        integer = 12,
        fraction = 2,
        message = "Use a valid amount with up to 2 decimals"
    )
    private BigDecimal amount;

    private boolean simulateFailure = false;
}