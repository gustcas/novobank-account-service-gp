package com.novobanco.accounts.domain.exception;

public final class AccountClosedException extends RuntimeException implements DomainException {

    private final String accountNumber;

    public AccountClosedException(String accountNumber) {
        super("Account " + accountNumber + " is closed and cannot perform operations");
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
