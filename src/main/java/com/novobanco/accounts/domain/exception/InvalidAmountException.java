package com.novobanco.accounts.domain.exception;

import java.math.BigDecimal;

public final class InvalidAmountException extends RuntimeException implements DomainException {

    public InvalidAmountException(BigDecimal amount) {
        super("Amount must be positive, got: " + amount);
    }
}
