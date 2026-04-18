package com.novobanco.accounts.application.usecase;

import com.novobanco.accounts.domain.exception.AccountBlockedException;
import com.novobanco.accounts.domain.exception.InsufficientFundsException;
import com.novobanco.accounts.domain.model.Account;
import com.novobanco.accounts.domain.model.AccountStatus;
import com.novobanco.accounts.domain.model.AccountType;
import com.novobanco.accounts.domain.model.Transaction;
import com.novobanco.accounts.domain.model.TransactionStatus;
import com.novobanco.accounts.domain.model.TransactionType;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService")
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private TransferService service;

    @BeforeEach
    void setUp() {
        service = new TransferService(accountRepository, transactionRepository);
    }

    @Test
    @DisplayName("should_transfer_atomically_when_both_accounts_are_active")
    void should_transfer_atomically_when_both_accounts_are_active() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Account source = buildAccount(sourceId, AccountStatus.ACTIVE, new BigDecimal("1000.00"));
        Account target = buildAccount(targetId, AccountStatus.ACTIVE, new BigDecimal("200.00"));

        stubLockForBothAccounts(sourceId, source, targetId, target);
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Transaction> result = service.transfer(sourceId, targetId, new BigDecimal("300.00"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo(TransactionType.TRANSFER_DEBIT);
        assertThat(result.get(1).getType()).isEqualTo(TransactionType.TRANSFER_CREDIT);
        assertThat(source.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(target.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("should_throw_InsufficientFundsException_when_source_has_insufficient_balance")
    void should_throw_InsufficientFundsException_when_source_has_insufficient_balance() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Account source = buildAccount(sourceId, AccountStatus.ACTIVE, new BigDecimal("50.00"));
        Account target = buildAccount(targetId, AccountStatus.ACTIVE, new BigDecimal("200.00"));

        stubLockForBothAccounts(sourceId, source, targetId, target);

        assertThatThrownBy(() -> service.transfer(sourceId, targetId, new BigDecimal("300.00")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    @DisplayName("should_throw_AccountBlockedException_when_target_is_blocked")
    void should_throw_AccountBlockedException_when_target_is_blocked() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Account source = buildAccount(sourceId, AccountStatus.ACTIVE, new BigDecimal("1000.00"));
        Account target = buildAccount(targetId, AccountStatus.BLOCKED, new BigDecimal("0.00"));

        stubLockForBothAccounts(sourceId, source, targetId, target);

        assertThatThrownBy(() -> service.transfer(sourceId, targetId, new BigDecimal("100.00")))
                .isInstanceOf(AccountBlockedException.class);
    }

    @Test
    @DisplayName("should_produce_debit_and_credit_transactions_with_matching_amounts")
    void should_produce_debit_and_credit_transactions_with_matching_amounts() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Account source = buildAccount(sourceId, AccountStatus.ACTIVE, new BigDecimal("500.00"));
        Account target = buildAccount(targetId, AccountStatus.ACTIVE, new BigDecimal("100.00"));

        stubLockForBothAccounts(sourceId, source, targetId, target);
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Transaction> result = service.transfer(sourceId, targetId, new BigDecimal("150.00"));

        Transaction debit = result.get(0);
        Transaction credit = result.get(1);
        assertThat(debit.getAmount()).isEqualByComparingTo(credit.getAmount());
        assertThat(debit.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(credit.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    private void stubLockForBothAccounts(UUID sourceId, Account source,
            UUID targetId, Account target) {
        when(accountRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(targetId)).thenReturn(Optional.of(target));
    }

    private Account buildAccount(UUID id, AccountStatus status, BigDecimal balance) {
        return new Account(id, UUID.randomUUID(), "ACC-TEST-" + id.toString().substring(0, 4),
                AccountType.SAVINGS, "USD", balance, status, OffsetDateTime.now());
    }
}
