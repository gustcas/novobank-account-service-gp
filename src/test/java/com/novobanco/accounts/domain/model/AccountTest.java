package com.novobanco.accounts.domain.model;

import com.novobanco.accounts.domain.exception.AccountBlockedException;
import com.novobanco.accounts.domain.exception.AccountClosedException;
import com.novobanco.accounts.domain.exception.InsufficientFundsException;
import com.novobanco.accounts.domain.exception.InvalidAmountException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Account domain model")
class AccountTest {

    private Account activeAccount;
    private Account blockedAccount;
    private Account closedAccount;

    @BeforeEach
    void setUp() {
        activeAccount = buildAccount(AccountStatus.ACTIVE, new BigDecimal("1000.00"));
        blockedAccount = buildAccount(AccountStatus.BLOCKED, new BigDecimal("500.00"));
        closedAccount = buildAccount(AccountStatus.CLOSED, new BigDecimal("0.00"));
    }

    @Nested
    @DisplayName("validateOperational()")
    class ValidateOperational {

        @Test
        @DisplayName("should_pass_when_account_is_active")
        void should_pass_when_account_is_active() {
            activeAccount.validateOperational();
        }

        @Test
        @DisplayName("should_throw_AccountBlockedException_when_account_is_blocked")
        void should_throw_AccountBlockedException_when_account_is_blocked() {
            assertThatThrownBy(() -> blockedAccount.validateOperational())
                    .isInstanceOf(AccountBlockedException.class)
                    .hasMessageContaining("blocked");
        }

        @Test
        @DisplayName("should_throw_AccountClosedException_when_account_is_closed")
        void should_throw_AccountClosedException_when_account_is_closed() {
            assertThatThrownBy(() -> closedAccount.validateOperational())
                    .isInstanceOf(AccountClosedException.class)
                    .hasMessageContaining("closed");
        }
    }

    @Nested
    @DisplayName("debit()")
    class Debit {

        @Test
        @DisplayName("should_reduce_balance_when_sufficient_funds")
        void should_reduce_balance_when_sufficient_funds() {
            activeAccount.debit(new BigDecimal("300.00"));

            assertThat(activeAccount.getBalance())
                    .isEqualByComparingTo(new BigDecimal("700.00"));
        }

        @Test
        @DisplayName("should_allow_debit_when_exact_balance")
        void should_allow_debit_when_exact_balance() {
            activeAccount.debit(new BigDecimal("1000.00"));

            assertThat(activeAccount.getBalance())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should_throw_InsufficientFundsException_when_balance_is_zero")
        void should_throw_InsufficientFundsException_when_balance_is_zero() {
            Account zeroBalance = buildAccount(AccountStatus.ACTIVE, BigDecimal.ZERO);

            assertThatThrownBy(() -> zeroBalance.debit(new BigDecimal("1.00")))
                    .isInstanceOf(InsufficientFundsException.class)
                    .satisfies(ex -> {
                        InsufficientFundsException ife = (InsufficientFundsException) ex;
                        assertThat(ife.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
                        assertThat(ife.getRequestedAmount()).isEqualByComparingTo(new BigDecimal("1.00"));
                    });
        }

        @Test
        @DisplayName("should_throw_InsufficientFundsException_when_amount_exceeds_balance")
        void should_throw_InsufficientFundsException_when_amount_exceeds_balance() {
            assertThatThrownBy(() -> activeAccount.debit(new BigDecimal("1000.01")))
                    .isInstanceOf(InsufficientFundsException.class);
        }

        @Test
        @DisplayName("should_throw_InvalidAmountException_when_amount_is_zero")
        void should_throw_InvalidAmountException_when_amount_is_zero() {
            assertThatThrownBy(() -> activeAccount.debit(BigDecimal.ZERO))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        @DisplayName("should_throw_InvalidAmountException_when_amount_is_negative")
        void should_throw_InvalidAmountException_when_amount_is_negative() {
            assertThatThrownBy(() -> activeAccount.debit(new BigDecimal("-50.00")))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        @DisplayName("should_throw_InvalidAmountException_when_amount_is_null")
        void should_throw_InvalidAmountException_when_amount_is_null() {
            assertThatThrownBy(() -> activeAccount.debit(null))
                    .isInstanceOf(InvalidAmountException.class);
        }
    }

    @Nested
    @DisplayName("credit()")
    class Credit {

        @Test
        @DisplayName("should_increase_balance_when_valid_amount")
        void should_increase_balance_when_valid_amount() {
            activeAccount.credit(new BigDecimal("500.00"));

            assertThat(activeAccount.getBalance())
                    .isEqualByComparingTo(new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("should_throw_InvalidAmountException_when_amount_is_zero")
        void should_throw_InvalidAmountException_when_amount_is_zero() {
            assertThatThrownBy(() -> activeAccount.credit(BigDecimal.ZERO))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        @DisplayName("should_throw_InvalidAmountException_when_amount_is_negative")
        void should_throw_InvalidAmountException_when_amount_is_negative() {
            assertThatThrownBy(() -> activeAccount.credit(new BigDecimal("-100.00")))
                    .isInstanceOf(InvalidAmountException.class);
        }
    }

    private Account buildAccount(AccountStatus status, BigDecimal balance) {
        return new Account(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                AccountType.SAVINGS,
                "USD",
                balance,
                status,
                OffsetDateTime.now()
        );
    }
}
