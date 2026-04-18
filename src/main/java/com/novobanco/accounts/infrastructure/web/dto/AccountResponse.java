package com.novobanco.accounts.infrastructure.web.dto;

import com.novobanco.accounts.domain.model.Account;
import com.novobanco.accounts.domain.model.AccountStatus;
import com.novobanco.accounts.domain.model.AccountType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID customerId,
        String accountNumber,
        AccountType type,
        String currency,
        BigDecimal balance,
        AccountStatus status,
        OffsetDateTime createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getAccountNumber(),
                account.getType(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}
