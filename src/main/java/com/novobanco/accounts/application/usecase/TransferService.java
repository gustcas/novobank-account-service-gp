package com.novobanco.accounts.application.usecase;

import com.novobanco.accounts.domain.exception.AccountNotFoundException;
import com.novobanco.accounts.domain.model.Account;
import com.novobanco.accounts.domain.model.Transaction;
import com.novobanco.accounts.domain.model.TransactionStatus;
import com.novobanco.accounts.domain.model.TransactionType;
import com.novobanco.accounts.domain.port.in.TransferUseCase;
import com.novobanco.accounts.domain.port.out.AccountRepository;
import com.novobanco.accounts.domain.port.out.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService implements TransferUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Escenario 3 (Transferencia atómica):
     * Todo ocurre dentro de un único @Transactional.
     * Si el crédito falla (cuenta destino bloqueada, etc.),
     * Spring revierte el débito automáticamente.
     *
     * Orden de locks para prevenir deadlocks:
     * Siempre adquirimos locks en orden ascendente de UUID.
     * Sin este orden: Thread-A bloquea cuenta-X y espera cuenta-Y;
     * Thread-B bloquea cuenta-Y y espera cuenta-X → deadlock.
     *
     * Escenario 3, pregunta de defensa:
     * "¿Qué pasa si la JVM cae entre el débito y el crédito?"
     * → @Transactional garantiza atomicidad: si la JVM cae sin hacer commit,
     * PostgreSQL revierte la transacción completa en el siguiente startup.
     * El débito nunca persiste sin el crédito correspondiente.
     */
    @Override
    @Transactional
    public List<Transaction> transfer(UUID sourceAccountId, UUID targetAccountId,
            BigDecimal amount) {
        UUID firstLockId = sourceAccountId.compareTo(targetAccountId) < 0
                ? sourceAccountId : targetAccountId;
        UUID secondLockId = firstLockId.equals(sourceAccountId)
                ? targetAccountId : sourceAccountId;

        Account firstAccount = accountRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new AccountNotFoundException(firstLockId));
        Account secondAccount = accountRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new AccountNotFoundException(secondLockId));

        Account source = firstLockId.equals(sourceAccountId) ? firstAccount : secondAccount;
        Account target = firstLockId.equals(targetAccountId) ? firstAccount : secondAccount;

        source.validateOperational();
        target.validateOperational();

        source.debit(amount);
        target.credit(amount);

        accountRepository.save(source);
        accountRepository.save(target);

        UUID debitRef = UUID.randomUUID();
        UUID creditRef = UUID.randomUUID();
        UUID debitId = UUID.randomUUID();
        UUID creditId = UUID.randomUUID();

        Transaction debitTx = new Transaction(
                debitId,
                sourceAccountId,
                TransactionType.TRANSFER_DEBIT,
                amount,
                TransactionStatus.SUCCESS,
                debitRef,
                creditId,
                null,
                OffsetDateTime.now()
        );

        Transaction creditTx = new Transaction(
                creditId,
                targetAccountId,
                TransactionType.TRANSFER_CREDIT,
                amount,
                TransactionStatus.SUCCESS,
                creditRef,
                debitId,
                null,
                OffsetDateTime.now()
        );

        Transaction savedDebit = transactionRepository.save(debitTx);
        Transaction savedCredit = transactionRepository.save(creditTx);

        return List.of(savedDebit, savedCredit);
    }
}
