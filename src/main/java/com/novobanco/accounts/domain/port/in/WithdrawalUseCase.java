package com.novobanco.accounts.domain.port.in;

import com.novobanco.accounts.domain.model.Transaction;

import java.math.BigDecimal;
import java.util.UUID;

public interface WithdrawalUseCase {

    Transaction withdraw(UUID accountId, BigDecimal amount);
}
