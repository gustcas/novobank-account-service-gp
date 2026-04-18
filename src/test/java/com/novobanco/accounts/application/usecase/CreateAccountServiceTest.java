package com.novobanco.accounts.application.usecase;

import com.novobanco.accounts.domain.model.Account;
import com.novobanco.accounts.domain.model.AccountStatus;
import com.novobanco.accounts.domain.model.AccountType;
import com.novobanco.accounts.domain.port.out.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAccountService")
class CreateAccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private CreateAccountService service;

    @BeforeEach
    void setUp() {
        service = new CreateAccountService(accountRepository);
    }

    @Test
    @DisplayName("should_create_account_with_zero_balance_when_valid_request")
    void should_create_account_with_zero_balance_when_valid_request() {
        UUID customerId = UUID.randomUUID();
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = service.createAccount(customerId, AccountType.SAVINGS);

        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(result.getType()).isEqualTo(AccountType.SAVINGS);
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getCustomerId()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("should_generate_unique_account_number_with_ACC_prefix")
    void should_generate_unique_account_number_with_ACC_prefix() {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = service.createAccount(UUID.randomUUID(), AccountType.CHECKING);

        assertThat(result.getAccountNumber()).startsWith("ACC-");
    }

    @Test
    @DisplayName("should_retry_account_number_generation_when_collision_occurs")
    void should_retry_account_number_generation_when_collision_occurs() {
        when(accountRepository.existsByAccountNumber(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        service.createAccount(UUID.randomUUID(), AccountType.SAVINGS);

        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getAccountNumber()).startsWith("ACC-");
    }

    @Test
    @DisplayName("should_persist_account_with_correct_customer_id")
    void should_persist_account_with_correct_customer_id() {
        UUID customerId = UUID.randomUUID();
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account acc = inv.getArgument(0);
            return new Account(acc.getId(), acc.getCustomerId(), acc.getAccountNumber(),
                    acc.getType(), acc.getCurrency(), acc.getBalance(), acc.getStatus(),
                    OffsetDateTime.now());
        });

        Account result = service.createAccount(customerId, AccountType.SAVINGS);

        assertThat(result.getCustomerId()).isEqualTo(customerId);
    }
}
