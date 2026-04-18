package com.novobanco.accounts.domain.exception;

import java.util.UUID;

public final class AccountNotFoundException extends RuntimeException implements DomainException {

    public AccountNotFoundException(UUID accountId) {
        super("Account not found with id: " + accountId);
    }

    public AccountNotFoundException(String accountNumber) {
        super("Account not found with number: " + accountNumber);
    }
}
