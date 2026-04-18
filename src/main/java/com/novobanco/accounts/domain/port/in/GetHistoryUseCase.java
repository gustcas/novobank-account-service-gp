package com.novobanco.accounts.domain.port.in;

import com.novobanco.accounts.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GetHistoryUseCase {

    Page<Transaction> getTransactionHistory(UUID accountId, Pageable pageable);
}
