package com.novobanco.accounts.domain.port.in;

import com.novobanco.accounts.domain.model.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TransferUseCase {

    /**
     * Transfiere fondos entre dos cuentas de forma atómica.
     * Retorna una lista con exactamente 2 transacciones:
     *   [0] = TRANSFER_DEBIT (cuenta origen)
     *   [1] = TRANSFER_CREDIT (cuenta destino)
     * Si cualquier parte falla, Spring revierte ambas mediante @Transactional.
     */
    List<Transaction> transfer(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount);
}
