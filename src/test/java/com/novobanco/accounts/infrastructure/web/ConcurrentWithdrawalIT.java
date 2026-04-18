package com.novobanco.accounts.infrastructure.web;

import com.novobanco.accounts.domain.model.AccountType;
import com.novobanco.accounts.infrastructure.web.dto.CreateAccountRequest;
import com.novobanco.accounts.infrastructure.web.dto.DepositRequest;
import com.novobanco.accounts.infrastructure.web.dto.WithdrawalRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Concurrent Withdrawal Integration Test")
class ConcurrentWithdrawalIT extends AbstractIntegrationTest {

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }

    /**
     * Escenario 4: Dos retiros simultáneos sobre el mismo saldo.
     * Con SELECT FOR UPDATE: solo uno tiene éxito, el otro
     * recibe 422 INSUFFICIENT_FUNDS. El saldo nunca es negativo.
     *
     * Sin SELECT FOR UPDATE: ambos leerían el saldo = 100,
     * ambos pasarían la validación, resultado: saldo = -100 (violación).
     */
    @Test
    @DisplayName("should_not_allow_negative_balance_under_concurrent_withdrawals")
    void should_not_allow_negative_balance_under_concurrent_withdrawals() throws Exception {
        String accountId = createAccountWithBalance(new BigDecimal("100.00"));

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Integer> withdrawTask = () ->
            given()
                .contentType(ContentType.JSON)
                .body(new WithdrawalRequest(new BigDecimal("100.00")))
            .when()
                .post("/accounts/" + accountId + "/transactions/withdrawals")
            .then()
                .extract().statusCode();

        List<Future<Integer>> futures = new ArrayList<>();
        futures.add(executor.submit(withdrawTask));
        futures.add(executor.submit(withdrawTask));

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> future : futures) {
            statuses.add(future.get());
        }

        executor.shutdown();

        long successCount = statuses.stream().filter(s -> s == 201).count();
        long failureCount = statuses.stream().filter(s -> s == 422).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);

        float finalBalance = given()
            .get("/accounts/" + accountId)
        .then()
            .statusCode(200)
            .extract().path("balance");

        assertThat(finalBalance).isGreaterThanOrEqualTo(0);
        assertThat(finalBalance).isEqualTo(0.0f);
    }

    private String createAccountWithBalance(BigDecimal initialBalance) {
        String accountId = given()
            .contentType(ContentType.JSON)
            .body(new CreateAccountRequest(UUID.randomUUID(), AccountType.SAVINGS))
        .when()
            .post("/accounts")
        .then()
            .statusCode(201)
            .extract().path("id");

        given()
            .contentType(ContentType.JSON)
            .body(new DepositRequest(initialBalance))
        .when()
            .post("/accounts/" + accountId + "/transactions/deposits")
        .then()
            .statusCode(201);

        return accountId;
    }
}
