package com.novobanco.accounts.domain.exception;

public final class AccountBlockedException extends RuntimeException implements DomainException {

    private final String accountNumber;

    public AccountBlockedException(String accountNumber) {
        super("Account " + accountNumber + " is blocked and cannot perform operations");
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
