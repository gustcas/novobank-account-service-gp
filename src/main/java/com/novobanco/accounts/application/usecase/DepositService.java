package com.novobanco.accounts.application.usecase;

import com.novobanco.accounts.domain.exception.AccountNotFoundException;
import com.novobanco.accounts.domain.model.Account;
import com.novobanco.accounts.domain.model.Transaction;
import com.novobanco.accounts.domain.model.TransactionStatus;
import com.novobanco.accounts.domain.model.TransactionType;
import com.novobanco.accounts.domain.port.in.DepositUseCase;
import com.novobanco.accounts.domain.port.out.AccountRepository;
import com.novobanco.accounts.domain.port.out.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepositService implements DepositUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Escenario 5 (Idempotencia): si se recibe idempotencyKey no nulo
     * y ya existe una transacción con esa clave, retornamos la transacción
     * existente sin procesar el depósito nuevamente.
     * Esto previene el double-charge por reintentos de red.
     */
    @Override
    @Transactional
    public Transaction deposit(UUID accountId, BigDecimal amount, UUID idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        account.validateOperational();
        account.credit(amount);
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                accountId,
                TransactionType.DEPOSIT,
                amount,
                TransactionStatus.SUCCESS,
                UUID.randomUUID(),
                null,
                idempotencyKey,
                OffsetDateTime.now()
        );

        return transactionRepository.save(transaction);
    }
}
