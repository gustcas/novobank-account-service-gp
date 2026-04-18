package com.novobanco.accounts.application.usecase;

import com.novobanco.accounts.domain.exception.AccountBlockedException;
import com.novobanco.accounts.domain.exception.AccountNotFoundException;
import com.novobanco.accounts.domain.model.Account;
import com.novobanco.accounts.domain.model.AccountStatus;
import com.novobanco.accounts.domain.model.AccountType;
import com.novobanco.accounts.domain.model.Transaction;
import com.novobanco.accounts.domain.model.TransactionStatus;
import com.novobanco.accounts.domain.port.out.AccountRepository;
import com.novobanco.accounts.domain.port.out.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepositService")
class DepositServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private DepositService service;

    @BeforeEach
    void setUp() {
        service = new DepositService(accountRepository, transactionRepository);
    }

    @Test
    @DisplayName("should_deposit_successfully_when_account_is_active")
    void should_deposit_successfully_when_account_is_active() {
        UUID accountId = UUID.randomUUID();
        Account account = buildAccount(accountId, AccountStatus.ACTIVE, new BigDecimal("100.00"));

        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = service.deposit(accountId, new BigDecimal("50.00"), null);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("should_throw_AccountNotFoundException_when_account_does_not_exist")
    void should_throw_AccountNotFoundException_when_account_does_not_exist() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deposit(accountId, new BigDecimal("100.00"), null))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("should_throw_AccountBlockedException_when_account_is_blocked")
    void should_throw_AccountBlockedException_when_account_is_blocked() {
        UUID accountId = UUID.randomUUID();
        Account blocked = buildAccount(accountId, AccountStatus.BLOCKED, new BigDecimal("200.00"));

        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(blocked));

        assertThatThrownBy(() -> service.deposit(accountId, new BigDecimal("50.00"), null))
                .isInstanceOf(AccountBlockedException.class);
    }

    @Test
    @DisplayName("should_return_existing_transaction_when_idempotency_key_already_used")
    void should_return_existing_transaction_when_idempotency_key_already_used() {
        UUID idempotencyKey = UUID.randomUUID();
        Transaction existing = buildTransaction();

        when(transactionRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existing));

        Transaction result = service.deposit(UUID.randomUUID(), new BigDecimal("100.00"),
                idempotencyKey);

        assertThat(result).isSameAs(existing);
        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    private Account buildAccount(UUID id, AccountStatus status, BigDecimal balance) {
        return new Account(id, UUID.randomUUID(), "ACC-TEST001", AccountType.SAVINGS,
                "USD", balance, status, OffsetDateTime.now());
    }

    private Transaction buildTransaction() {
        return new Transaction(UUID.randomUUID(), UUID.randomUUID(),
                com.novobanco.accounts.domain.model.TransactionType.DEPOSIT,
                new BigDecimal("100.00"), TransactionStatus.SUCCESS,
                UUID.randomUUID(), null, UUID.randomUUID(), OffsetDateTime.now());
    }
}
