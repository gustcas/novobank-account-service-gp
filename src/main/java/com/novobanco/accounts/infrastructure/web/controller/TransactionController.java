package com.novobanco.accounts.infrastructure.web.controller;

import com.novobanco.accounts.domain.model.Transaction;
import com.novobanco.accounts.domain.port.in.DepositUseCase;
import com.novobanco.accounts.domain.port.in.GetHistoryUseCase;
import com.novobanco.accounts.domain.port.in.TransferUseCase;
import com.novobanco.accounts.domain.port.in.WithdrawalUseCase;
import com.novobanco.accounts.infrastructure.web.dto.DepositRequest;
import com.novobanco.accounts.infrastructure.web.dto.PageResponse;
import com.novobanco.accounts.infrastructure.web.dto.TransactionResponse;
import com.novobanco.accounts.infrastructure.web.dto.TransferRequest;
import com.novobanco.accounts.infrastructure.web.dto.WithdrawalRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction operations: deposit, withdrawal, transfer")
public class TransactionController {

    private final DepositUseCase depositUseCase;
    private final WithdrawalUseCase withdrawalUseCase;
    private final TransferUseCase transferUseCase;
    private final GetHistoryUseCase getHistoryUseCase;

    @PostMapping("/deposits")
    @Operation(summary = "Deposit funds to an account")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable UUID accountId,
            @Valid @RequestBody DepositRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey) {
        Transaction transaction = depositUseCase.deposit(accountId, request.amount(),
                idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TransactionResponse.from(transaction));
    }

    @PostMapping("/withdrawals")
    @Operation(summary = "Withdraw funds from an account")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable UUID accountId,
            @Valid @RequestBody WithdrawalRequest request) {
        Transaction transaction = withdrawalUseCase.withdraw(accountId, request.amount());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TransactionResponse.from(transaction));
    }

    @PostMapping("/transfers")
    @Operation(summary = "Transfer funds between two accounts (atomic)")
    public ResponseEntity<List<TransactionResponse>> transfer(
            @PathVariable UUID accountId,
            @Valid @RequestBody TransferRequest request) {
        List<Transaction> transactions = transferUseCase.transfer(
                accountId, request.targetAccountId(), request.amount());
        List<TransactionResponse> response = transactions.stream()
                .map(TransactionResponse::from)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get paginated transaction history for an account")
    public ResponseEntity<PageResponse<TransactionResponse>> getHistory(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> history = getHistoryUseCase.getTransactionHistory(accountId, pageable);
        return ResponseEntity.ok(PageResponse.from(history, TransactionResponse::from));
    }
}
