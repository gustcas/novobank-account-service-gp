package com.novobanco.accounts.domain.exception;

import java.math.BigDecimal;

public final class InsufficientFundsException extends RuntimeException implements DomainException {

    private final String accountNumber;
    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsException(String accountNumber, BigDecimal currentBalance,
            BigDecimal requestedAmount) {
        super(String.format(
            "Account %s has balance %.2f, requested %.2f",
            accountNumber, currentBalance, requestedAmount
        ));
        this.accountNumber = accountNumber;
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}
