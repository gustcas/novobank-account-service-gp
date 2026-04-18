package com.novobanco.accounts.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Value object de solo lectura.
 * Las transacciones son inmutables una vez creadas — el registro
 * financiero no se modifica; los estados finales (REVERSED) se
 * registran como nuevas filas o actualizaciones de status.
 */
public class Transaction {

    private final UUID id;
    private final UUID accountId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final TransactionStatus status;
    private final UUID reference;
    private final UUID relatedTransactionId;
    private final UUID idempotencyKey;
    private final OffsetDateTime createdAt;

    public Transaction(UUID id, UUID accountId, TransactionType type, BigDecimal amount,
            TransactionStatus status, UUID reference, UUID relatedTransactionId,
            UUID idempotencyKey, OffsetDateTime createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.reference = reference;
        this.relatedTransactionId = relatedTransactionId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public UUID getReference() {
        return reference;
    }

    public UUID getRelatedTransactionId() {
        return relatedTransactionId;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
