package com.novobanco.accounts.infrastructure.web.controller;

import com.novobanco.accounts.domain.model.Account;
import com.novobanco.accounts.domain.port.in.CreateAccountUseCase;
import com.novobanco.accounts.domain.port.in.GetAccountUseCase;
import com.novobanco.accounts.infrastructure.web.dto.AccountResponse;
import com.novobanco.accounts.infrastructure.web.dto.CreateAccountRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account management operations")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;

    @PostMapping
    @Operation(summary = "Create a new bank account")
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        Account account = createAccountUseCase.createAccount(
                request.customerId(),
                request.type()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID accountId) {
        Account account = getAccountUseCase.getAccountById(accountId);
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @GetMapping("/number/{accountNumber}")
    @Operation(summary = "Get account by account number")
    public ResponseEntity<AccountResponse> getAccountByNumber(
            @PathVariable String accountNumber) {
        Account account = getAccountUseCase.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(AccountResponse.from(account));
    }
}
