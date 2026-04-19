package com.novobanco.accounts.infrastructure.persistence.adapter;

import com.novobanco.accounts.domain.model.Account;
import com.novobanco.accounts.domain.model.AccountStatus;
import com.novobanco.accounts.domain.model.AccountType;
import com.novobanco.accounts.domain.port.out.AccountRepository;
import com.novobanco.accounts.infrastructure.persistence.entity.AccountEntity;
import com.novobanco.accounts.infrastructure.persistence.repository.JpaAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements AccountRepository {

    private final JpaAccountRepository jpaRepository;

    @Override
    public Account save(Account account) {
        AccountEntity entity = toEntity(account);
        AccountEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Account> findById(UUID accountId) {
        return jpaRepository.findById(accountId).map(this::toDomain);
    }

    @Override
    public List<Account> findByCustomerId(UUID customerId) {
        return jpaRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return jpaRepository.findByAccountNumber(accountNumber).map(this::toDomain);
    }

    @Override
    public Optional<Account> findByIdForUpdate(UUID accountId) {
        return jpaRepository.findByIdForUpdate(accountId).map(this::toDomain);
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return jpaRepository.existsByAccountNumber(accountNumber);
    }

    private AccountEntity toEntity(Account account) {
        return AccountEntity.builder()
                .id(account.getId())
                .customerId(account.getCustomerId())
                .accountNumber(account.getAccountNumber())
                .type(AccountEntity.AccountTypeJpa.valueOf(account.getType().name()))
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .status(AccountEntity.AccountStatusJpa.valueOf(account.getStatus().name()))
                .createdAt(account.getCreatedAt())
                .build();
    }

    private Account toDomain(AccountEntity entity) {
        return new Account(
                entity.getId(),
                entity.getCustomerId(),
                entity.getAccountNumber(),
                AccountType.valueOf(entity.getType().name()),
                entity.getCurrency(),
                entity.getBalance(),
                AccountStatus.valueOf(entity.getStatus().name()),
                entity.getCreatedAt()
        );
    }
}
