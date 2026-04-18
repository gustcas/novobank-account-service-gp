package com.novobanco.accounts.domain.exception;

import java.util.UUID;

public final class TransactionNotFoundException extends RuntimeException implements DomainException {

    public TransactionNotFoundException(UUID reference) {
        super("Transaction not found with reference: " + reference);
    }
}
