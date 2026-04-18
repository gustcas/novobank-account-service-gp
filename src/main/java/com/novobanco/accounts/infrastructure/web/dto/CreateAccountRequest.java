package com.novobanco.accounts.infrastructure.web.dto;

import com.novobanco.accounts.domain.model.AccountType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAccountRequest(
        @NotNull(message = "customerId is required")
        UUID customerId,

        @NotNull(message = "type is required")
        AccountType type
) {
}
