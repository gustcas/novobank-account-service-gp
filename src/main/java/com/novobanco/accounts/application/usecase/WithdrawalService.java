package com.novobanco.accounts.application.usecase;

import com.novobanco.accounts.domain.exception.AccountNotFoundException;
import com.novobanco.accounts.domain.model.Account;
import com.novobanco.accounts.domain.model.Transaction;
import com.novobanco.accounts.domain.model.TransactionStatus;
import com.novobanco.accounts.domain.model.TransactionType;
import com.novobanco.accounts.domain.port.in.WithdrawalUseCase;
import com.novobanco.accounts.domain.port.out.AccountRepository;
import com.novobanco.accounts.domain.port.out.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WithdrawalService implements WithdrawalUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Escenario 4 (Concurrencia): findByIdForUpdate obtiene SELECT FOR UPDATE.
     * El segundo retiro concurrente bloquea aquí hasta que el primero
     * haga commit y libere el lock. Luego relee el saldo actualizado.
     * Resultado: nunca hay saldo negativo aunque ambos retiros sean válidos
     * evaluados individualmente al mismo tiempo.
     */
    @Override
    @Transactional
    public Transaction withdraw(UUID accountId, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        account.validateOperational();
        account.debit(amount);
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                accountId,
                TransactionType.WITHDRAWAL,
                amount,
                TransactionStatus.SUCCESS,
                UUID.randomUUID(),
                null,
                null,
                OffsetDateTime.now()
        );

        return transactionRepository.save(transaction);
    }
}
