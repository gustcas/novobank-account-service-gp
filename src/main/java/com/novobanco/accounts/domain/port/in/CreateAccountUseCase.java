package com.novobanco.accounts.domain.port.in;

import com.novobanco.accounts.domain.model.Account;
import com.novobanco.accounts.domain.model.AccountType;

import java.util.UUID;

public interface CreateAccountUseCase {

    Account createAccount(UUID customerId, AccountType type);
}
