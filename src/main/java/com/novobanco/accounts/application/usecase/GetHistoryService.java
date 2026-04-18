package com.novobanco.accounts.application.usecase;

import com.novobanco.accounts.domain.model.Transaction;
import com.novobanco.accounts.domain.port.in.GetHistoryUseCase;
import com.novobanco.accounts.domain.port.out.AccountRepository;
import com.novobanco.accounts.domain.port.out.TransactionRepository;
import com.novobanco.accounts.domain.exception.AccountNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetHistoryService implements GetHistoryUseCase {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionHistory(UUID accountId, Pageable pageable) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        return transactionRepository.findByAccountId(accountId, pageable);
    }
}
