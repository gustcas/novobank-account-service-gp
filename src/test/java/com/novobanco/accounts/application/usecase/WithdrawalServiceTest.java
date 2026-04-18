package com.novobanco.accounts.application.usecase;

import com.novobanco.accounts.domain.exception.AccountBlockedException;
import com.novobanco.accounts.domain.exception.AccountClosedException;
import com.novobanco.accounts.domain.exception.AccountNotFoundException;
import com.novobanco.accounts.domain.exception.InsufficientFundsException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawalService")
class WithdrawalServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private WithdrawalService service;

    @BeforeEach
    void setUp() {
        service = new WithdrawalService(accountRepository, transactionRepository);
    }

    @Test
    @DisplayName("should_withdraw_successfully_when_sufficient_balance")
    void should_withdraw_successfully_when_sufficient_balance() {
        UUID accountId = UUID.randomUUID();
        Account account = buildAccount(accountId, AccountStatus.ACTIVE, new BigDecimal("500.00"));

        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = service.withdraw(accountId, new BigDecimal("200.00"));

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("should_throw_InsufficientFundsException_when_balance_is_zero")
    void should_throw_InsufficientFundsException_when_balance_is_zero() {
        UUID accountId = UUID.randomUUID();
        Account account = buildAccount(accountId, AccountStatus.ACTIVE, BigDecimal.ZERO);

        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.withdraw(accountId, new BigDecimal("1.00")))
                .isInstanceOf(InsufficientFundsException.class)
                .satisfies(ex -> {
                    InsufficientFundsException ife = (InsufficientFundsException) ex;
                    assertThat(ife.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
                });
    }

    @Test
    @DisplayName("should_throw_InsufficientFundsException_when_amount_exceeds_balance")
    void should_throw_InsufficientFundsException_when_amount_exceeds_balance() {
        UUID accountId = UUID.randomUUID();
        Account account = buildAccount(accountId, AccountStatus.ACTIVE, new BigDecimal("50.00"));

        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.withdraw(accountId, new BigDecimal("200.00")))
                .isInstanceOf(InsufficientFundsException.class)
                .satisfies(ex -> {
                    InsufficientFundsException ife = (InsufficientFundsException) ex;
                    assertThat(ife.getRequestedAmount())
                            .isEqualByComparingTo(new BigDecimal("200.00"));
                    assertThat(ife.getCurrentBalance())
                            .isEqualByComparingTo(new BigDecimal("50.00"));
                });
    }

    @Test
    @DisplayName("should_throw_AccountBlockedException_when_account_is_blocked")
    void should_throw_AccountBlockedException_when_account_is_blocked() {
        UUID accountId = UUID.randomUUID();
        Account blocked = buildAccount(accountId, AccountStatus.BLOCKED, new BigDecimal("1000.00"));

        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(blocked));

        assertThatThrownBy(() -> service.withdraw(accountId, new BigDecimal("100.00")))
                .isInstanceOf(AccountBlockedException.class);
    }

    @Test
    @DisplayName("should_throw_AccountClosedException_when_account_is_closed")
    void should_throw_AccountClosedException_when_account_is_closed() {
        UUID accountId = UUID.randomUUID();
        Account closed = buildAccount(accountId, AccountStatus.CLOSED, new BigDecimal("1000.00"));

        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> service.withdraw(accountId, new BigDecimal("100.00")))
                .isInstanceOf(AccountClosedException.class);
    }

    @Test
    @DisplayName("should_throw_AccountNotFoundException_when_account_does_not_exist")
    void should_throw_AccountNotFoundException_when_account_does_not_exist() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.withdraw(accountId, new BigDecimal("100.00")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("should_allow_withdrawal_of_exact_balance")
    void should_allow_withdrawal_of_exact_balance() {
        UUID accountId = UUID.randomUUID();
        Account account = buildAccount(accountId, AccountStatus.ACTIVE, new BigDecimal("100.00"));

        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.withdraw(accountId, new BigDecimal("100.00"));

        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Account buildAccount(UUID id, AccountStatus status, BigDecimal balance) {
        return new Account(id, UUID.randomUUID(), "ACC-TEST001", AccountType.SAVINGS,
                "USD", balance, status, OffsetDateTime.now());
    }
}
