package com.novobanco.accounts.domain.port.in;

import com.novobanco.accounts.domain.model.Account;

import java.util.List;
import java.util.UUID;

public interface GetAccountUseCase {

    List<Account> getAccountsByCustomerId(UUID customerId);

    Account getAccountById(UUID accountId);

    Account getAccountByNumber(String accountNumber);
}
