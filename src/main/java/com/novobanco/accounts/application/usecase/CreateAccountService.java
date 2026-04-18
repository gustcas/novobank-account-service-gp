package com.novobanco.accounts.application.usecase;

import com.novobanco.accounts.domain.model.Account;
import com.novobanco.accounts.domain.model.AccountStatus;
import com.novobanco.accounts.domain.model.AccountType;
import com.novobanco.accounts.domain.port.in.CreateAccountUseCase;
import com.novobanco.accounts.domain.port.out.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateAccountService implements CreateAccountUseCase {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public Account createAccount(UUID customerId, AccountType type) {
        String accountNumber = generateUniqueAccountNumber();

        Account account = new Account(
                UUID.randomUUID(),
                customerId,
                accountNumber,
                type,
                "USD",
                BigDecimal.ZERO,
                AccountStatus.ACTIVE,
                OffsetDateTime.now()
        );

        return accountRepository.save(account);
    }

    /**
     * Genera un número de cuenta con formato ACC-XXXXXXXXXX.
     * Reintenta si hay colisión (probabilidad astronomicamente baja con UUID).
     * En producción: sustituir por una secuencia de PostgreSQL para mayor eficiencia.
     */
    private String generateUniqueAccountNumber() {
        String candidate;
        do {
            candidate = "ACC-" + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 10)
                    .toUpperCase();
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }
}
