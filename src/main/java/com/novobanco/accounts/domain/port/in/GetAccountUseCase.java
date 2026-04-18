package com.novobanco.accounts.domain.port.in;

import com.novobanco.accounts.domain.model.Account;

import java.util.UUID;

public interface GetAccountUseCase {

    Account getAccountById(UUID accountId);

    Account getAccountByNumber(String accountNumber);
}
