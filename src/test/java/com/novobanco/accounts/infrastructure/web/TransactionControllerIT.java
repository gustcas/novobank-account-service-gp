package com.novobanco.accounts.infrastructure.web;

import com.novobanco.accounts.domain.model.AccountType;
import com.novobanco.accounts.infrastructure.web.dto.CreateAccountRequest;
import com.novobanco.accounts.infrastructure.web.dto.DepositRequest;
import com.novobanco.accounts.infrastructure.web.dto.TransferRequest;
import com.novobanco.accounts.infrastructure.web.dto.WithdrawalRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("TransactionController Integration Tests")
class TransactionControllerIT extends AbstractIntegrationTest {

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }

    @Test
    @DisplayName("should_deposit_successfully_and_update_balance")
    void should_deposit_successfully_and_update_balance() {
        String accountId = createAccount(AccountType.SAVINGS);

        given()
            .contentType(ContentType.JSON)
            .body(new DepositRequest(new BigDecimal("500.00")))
        .when()
            .post("/accounts/" + accountId + "/transactions/deposits")
        .then()
            .statusCode(201)
            .body("type", equalTo("DEPOSIT"))
            .body("status", equalTo("SUCCESS"))
            .body("reference", notNullValue());

        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/accounts/" + accountId)
        .then()
            .statusCode(200)
            .body("balance", equalTo(500.0f));
    }

    @Test
    @DisplayName("should_return_422_when_withdrawal_exceeds_balance")
    void should_return_422_when_withdrawal_exceeds_balance() {
        String accountId = createAccount(AccountType.SAVINGS);
        deposit(accountId, new BigDecimal("100.00"));

        given()
            .contentType(ContentType.JSON)
            .body(new WithdrawalRequest(new BigDecimal("500.00")))
        .when()
            .post("/accounts/" + accountId + "/transactions/withdrawals")
        .then()
            .statusCode(422)
            .body("errorCode", equalTo("INSUFFICIENT_FUNDS"));
    }

    @Test
    @DisplayName("should_transfer_atomically_between_two_accounts")
    void should_transfer_atomically_between_two_accounts() {
        String sourceId = createAccount(AccountType.SAVINGS);
        String targetId = createAccount(AccountType.CHECKING);
        deposit(sourceId, new BigDecimal("1000.00"));

        given()
            .contentType(ContentType.JSON)
            .body(new TransferRequest(UUID.fromString(targetId), new BigDecimal("300.00")))
        .when()
            .post("/accounts/" + sourceId + "/transactions/transfers")
        .then()
            .statusCode(201)
            .body("$", hasSize(2))
            .body("[0].type", equalTo("TRANSFER_DEBIT"))
            .body("[1].type", equalTo("TRANSFER_CREDIT"));

        given().get("/accounts/" + sourceId).then().body("balance", equalTo(700.0f));
        given().get("/accounts/" + targetId).then().body("balance", equalTo(300.0f));
    }

    @Test
    @DisplayName("should_return_paginated_transaction_history")
    void should_return_paginated_transaction_history() {
        String accountId = createAccount(AccountType.SAVINGS);
        deposit(accountId, new BigDecimal("100.00"));
        deposit(accountId, new BigDecimal("200.00"));

        given()
            .contentType(ContentType.JSON)
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/accounts/" + accountId + "/transactions")
        .then()
            .statusCode(200)
            .body("content", hasSize(2))
            .body("totalElements", equalTo(2))
            .body("page", equalTo(0));
    }

    @Test
    @DisplayName("should_return_idempotent_response_for_same_idempotency_key")
    void should_return_idempotent_response_for_same_idempotency_key() {
        String accountId = createAccount(AccountType.SAVINGS);
        UUID idempotencyKey = UUID.randomUUID();

        String firstRef = given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", idempotencyKey.toString())
            .body(new DepositRequest(new BigDecimal("100.00")))
        .when()
            .post("/accounts/" + accountId + "/transactions/deposits")
        .then()
            .statusCode(201)
            .extract().path("reference");

        String secondRef = given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", idempotencyKey.toString())
            .body(new DepositRequest(new BigDecimal("100.00")))
        .when()
            .post("/accounts/" + accountId + "/transactions/deposits")
        .then()
            .statusCode(201)
            .extract().path("reference");

        // Misma transacción retornada — el saldo solo se incrementó una vez
        assert firstRef.equals(secondRef) : "Idempotency key should return same transaction";

        given().get("/accounts/" + accountId).then().body("balance", equalTo(100.0f));
    }

    private String createAccount(AccountType type) {
        return given()
            .contentType(ContentType.JSON)
            .body(new CreateAccountRequest(UUID.randomUUID(), type))
        .when()
            .post("/accounts")
        .then()
            .statusCode(201)
            .extract().path("id");
    }

    private void deposit(String accountId, BigDecimal amount) {
        given()
            .contentType(ContentType.JSON)
            .body(new DepositRequest(amount))
        .when()
            .post("/accounts/" + accountId + "/transactions/deposits")
        .then()
            .statusCode(201);
    }
}
