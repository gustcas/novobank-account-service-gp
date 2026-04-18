package com.novobanco.accounts.infrastructure.persistence.adapter;

import com.novobanco.accounts.domain.model.Transaction;
import com.novobanco.accounts.domain.model.TransactionStatus;
import com.novobanco.accounts.domain.model.TransactionType;
import com.novobanco.accounts.domain.port.out.TransactionRepository;
import com.novobanco.accounts.infrastructure.persistence.entity.AccountEntity;
import com.novobanco.accounts.infrastructure.persistence.entity.TransactionEntity;
import com.novobanco.accounts.infrastructure.persistence.repository.JpaAccountRepository;
import com.novobanco.accounts.infrastructure.persistence.repository.JpaTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionPersistenceAdapter implements TransactionRepository {

    private final JpaTransactionRepository jpaRepository;
    private final JpaAccountRepository jpaAccountRepository;

    @Override
    public Transaction save(Transaction transaction) {
        AccountEntity accountRef = jpaAccountRepository.getReferenceById(transaction.getAccountId());
        TransactionEntity entity = toEntity(transaction, accountRef);
        TransactionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Transaction> findByReference(UUID reference) {
        return jpaRepository.findByReference(reference).map(this::toDomain);
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(UUID idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public Page<Transaction> findByAccountId(UUID accountId, Pageable pageable) {
        return jpaRepository.findByAccountId(accountId, pageable).map(this::toDomain);
    }

    private TransactionEntity toEntity(Transaction transaction, AccountEntity accountRef) {
        return TransactionEntity.builder()
                .id(transaction.getId())
                .account(accountRef)
                .type(TransactionEntity.TransactionTypeJpa.valueOf(transaction.getType().name()))
                .amount(transaction.getAmount())
                .status(TransactionEntity.TransactionStatusJpa.valueOf(transaction.getStatus().name()))
                .reference(transaction.getReference())
                .relatedTransactionId(transaction.getRelatedTransactionId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private Transaction toDomain(TransactionEntity entity) {
        return new Transaction(
                entity.getId(),
                entity.getAccount().getId(),
                TransactionType.valueOf(entity.getType().name()),
                entity.getAmount(),
                TransactionStatus.valueOf(entity.getStatus().name()),
                entity.getReference(),
                entity.getRelatedTransactionId(),
                entity.getIdempotencyKey(),
                entity.getCreatedAt()
        );
    }
}
