package com.novobanco.accounts.infrastructure.web.dto;

import com.novobanco.accounts.domain.model.Transaction;
import com.novobanco.accounts.domain.model.TransactionStatus;
import com.novobanco.accounts.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID accountId,
        TransactionType type,
        BigDecimal amount,
        TransactionStatus status,
        UUID reference,
        UUID relatedTransactionId,
        OffsetDateTime createdAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getAccountId(),
                tx.getType(),
                tx.getAmount(),
                tx.getStatus(),
                tx.getReference(),
                tx.getRelatedTransactionId(),
                tx.getCreatedAt()
        );
    }
}
