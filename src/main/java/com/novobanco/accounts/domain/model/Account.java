package com.novobanco.accounts.domain.model;

import com.novobanco.accounts.domain.exception.AccountBlockedException;
import com.novobanco.accounts.domain.exception.AccountClosedException;
import com.novobanco.accounts.domain.exception.InsufficientFundsException;
import com.novobanco.accounts.domain.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Aggregate root del dominio de cuentas.
 * POJO puro: sin @Entity, sin @Component, sin dependencia de frameworks.
 * Todas las invariantes de negocio están protegidas por los métodos de este objeto.
 *
 * Invariantes que este objeto garantiza:
 * 1. balance >= 0 en todo momento
 * 2. Solo cuentas ACTIVE pueden operar
 * 3. Los montos de operación deben ser positivos
 */
public class Account {

    private final UUID id;
    private final UUID customerId;
    private final String accountNumber;
    private final AccountType type;
    private final String currency;
    private BigDecimal balance;
    private AccountStatus status;
    private final OffsetDateTime createdAt;

    public Account(UUID id, UUID customerId, String accountNumber, AccountType type,
            String currency, BigDecimal balance, AccountStatus status, OffsetDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Verifica que la cuenta puede operar.
     * Lanza excepción específica por estado — nunca un error genérico.
     * Esto permite al manejador HTTP retornar ACCOUNT_BLOCKED vs ACCOUNT_CLOSED.
     */
    public void validateOperational() {
        if (status == AccountStatus.BLOCKED) {
            throw new AccountBlockedException(accountNumber);
        }
        if (status == AccountStatus.CLOSED) {
            throw new AccountClosedException(accountNumber);
        }
    }

    /**
     * Acredita fondos a la cuenta.
     * Pre-condición: cuenta ACTIVE (validada externamente via validateOperational()).
     */
    public void credit(BigDecimal amount) {
        validatePositiveAmount(amount);
        this.balance = this.balance.add(amount);
    }

    /**
     * Debita fondos de la cuenta.
     * Valida saldo suficiente antes del débito — primera línea de defensa.
     * La segunda defensa es el CHECK constraint en PostgreSQL.
     *
     * Razón de doble capa:
     * - Capa aplicación: mensaje de error rico con contexto (cuenta, saldo actual, monto)
     * - Capa DB: seguridad ante bugs en el código que omitan la validación de aplicación
     */
    public void debit(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountNumber, balance, amount);
        }
        this.balance = this.balance.subtract(amount);
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(amount);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountType getType() {
        return type;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
