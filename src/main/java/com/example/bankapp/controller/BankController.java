package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class BankController {

    private static final Logger log =
        LoggerFactory.getLogger(BankController.class);

    private final BankService bankService;
    private final TransactionAuditService auditService;

    // --------------------------------
    // Load dashboard data
    // --------------------------------

    private void loadDashboard(Model model, String keyword) {

        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute(
                "transferRequest",
                new TransferRequest()
            );
        }

        if (!model.containsAttribute("customerForm")) {
            model.addAttribute(
                "customerForm",
                new Customer()
            );
        }

        model.addAttribute("keyword", keyword);

        // Filtered customers for the search results table
        model.addAttribute(
            "customers",
            bankService.getCustomers(keyword)
        );

        // All customers for the transfer dropdowns
        model.addAttribute(
            "allCustomers",
            bankService.getCustomers("")
        );

        model.addAttribute(
            "transactions",
            bankService.getLatestTransactions()
        );

        model.addAttribute(
            "totalBalance",
            bankService.getTotalBalance()
        );

        model.addAttribute(
            "customerCount",
            bankService.getCustomerCount()
        );

        model.addAttribute(
            "successCount",
            bankService.countTransactions(
                TransactionStatus.SUCCESS
            )
        );

        model.addAttribute(
            "failedCount",
            bankService.countTransactions(
                TransactionStatus.FAILED
            )
        );

        model.addAttribute(
            "feeEnabled",
            bankService.isFeeEnabled()
        );
    }

    // --------------------------------
    // GET / - Dashboard
    // --------------------------------

    @GetMapping("/")
    public String dashboard(
            @RequestParam(defaultValue = "") String keyword,
            Model model) {

        loadDashboard(model, keyword);

        return "bank";
    }

    // --------------------------------
    // POST /customers
    // --------------------------------

    @PostMapping("/customers")
    public String createCustomer(
            @Valid @ModelAttribute("customerForm") Customer customer,
            BindingResult result,
            Model model,
            RedirectAttributes redirect) {

        if (result.hasErrors()) {

            loadDashboard(model, "");

            return "bank";
        }

        try {

            bankService.createCustomer(customer);

            redirect.addFlashAttribute(
                "success",
                "Customer added successfully."
            );

        } catch (Exception ex) {

            redirect.addFlashAttribute(
                "error",
                "Could not add customer. " + safeMessage(ex)
            );
        }

        return "redirect:/";
    }

    // --------------------------------
    // POST /transfer
    // --------------------------------

    @PostMapping("/transfer")
    public String transfer(
            @Valid @ModelAttribute("transferRequest")
                TransferRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirect) {

        // Validation errors: no bank operation is executed
        if (result.hasErrors()) {

            loadDashboard(model, "");

            return "bank";
        }

        try {

            bankService.transfer(request);

            redirect.addFlashAttribute(
                "success",
                "Money transferred successfully."
            );

        } catch (Exception ex) {

            // Main transfer has already rolled back.
            // Save FAILED record independently.
            recordFailure(
                request.getFromAccount(),
                request.getToAccount(),
                request.getAmount(),
                "TRANSFER",
                safeMessage(ex)
            );

            redirect.addFlashAttribute(
                "error",
                safeMessage(ex)
            );
        }

        return "redirect:/";
    }

    // --------------------------------
    // GET /accounts/{accountNumber}/history
    // --------------------------------

    @GetMapping("/accounts/{accountNumber}/history")
    public String history(
            @PathVariable String accountNumber,
            Model model,
            RedirectAttributes redirect) {

        try {

            model.addAttribute(
                "customer",
                bankService.getCustomer(accountNumber)
            );

            model.addAttribute(
                "transactions",
                bankService.getHistory(accountNumber)
            );

            model.addAttribute(
                "totalSent",
                bankService.getTotalSent(accountNumber)
            );

            return "history";

        } catch (Exception ex) {

            redirect.addFlashAttribute(
                "error",
                safeMessage(ex)
            );

            return "redirect:/";
        }
    }

    // --------------------------------
    // GET /large-transfers?min=10000
    // --------------------------------

    @GetMapping("/large-transfers")
    public String largeTransfers(
            @RequestParam(defaultValue = "10000") BigDecimal min,
            Model model) {

        model.addAttribute(
            "min",
            min
        );

        model.addAttribute(
            "transactions",
            bankService.getLargeTransfers(min)
        );

        return "large-transfers";
    }

    // --------------------------------
    // GET /deposit
    // --------------------------------

    @GetMapping("/deposit")
    public String depositPage(Model model) {

        model.addAttribute(
            "customers",
            bankService.getCustomers("")
        );

        return "deposit";
    }

    // --------------------------------
    // POST /deposit
    // --------------------------------

    @PostMapping("/deposit")
    public String deposit(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirect) {

        try {

            bankService.deposit(
                accountNumber,
                amount
            );

            redirect.addFlashAttribute(
                "success",
                "Deposit completed successfully."
            );

        } catch (Exception ex) {

            recordFailure(
                "CASH",
                accountNumber,
                amount,
                "DEPOSIT",
                safeMessage(ex)
            );

            redirect.addFlashAttribute(
                "error",
                safeMessage(ex)
            );
        }

        return "redirect:/deposit";
    }

    // --------------------------------
    // GET /withdraw
    // --------------------------------

    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {

        model.addAttribute(
            "customers",
            bankService.getCustomers("")
        );

        return "withdraw";
    }

    // --------------------------------
    // POST /withdraw
    // --------------------------------

    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirect) {

        try {

            bankService.withdraw(
                accountNumber,
                amount
            );

            redirect.addFlashAttribute(
                "success",
                "Withdrawal completed successfully."
            );

        } catch (Exception ex) {

            recordFailure(
                accountNumber,
                "CASH",
                amount,
                "WITHDRAW",
                safeMessage(ex)
            );

            redirect.addFlashAttribute(
                "error",
                safeMessage(ex)
            );
        }

        return "redirect:/withdraw";
    }

    // --------------------------------
    // Record FAILED transaction
    // --------------------------------

    private void recordFailure(
            String from,
            String to,
            BigDecimal amount,
            String type,
            String reason) {

        try {

            auditService.recordFailure(
                from,
                to,
                amount,
                type,
                reason
            );

        } catch (Exception auditEx) {

            log.error(
                "Could not save FAILED transaction audit.",
                auditEx
            );
        }
    }

    // --------------------------------
    // Safe error messages
    // --------------------------------

    private String safeMessage(Exception ex) {

        if (ex instanceof BankException) {
            return ex.getMessage();
        }

        log.error("Bank operation failed.", ex);

        return "Operation failed due to an unexpected error.";
    }
}