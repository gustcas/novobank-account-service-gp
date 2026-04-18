package com.novobanco.accounts.infrastructure.web;

import com.novobanco.accounts.domain.model.AccountType;
import com.novobanco.accounts.infrastructure.web.dto.CreateAccountRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@DisplayName("AccountController Integration Tests")
class AccountControllerIT extends AbstractIntegrationTest {

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }

    @Test
    @DisplayName("should_create_account_and_return_201_with_account_number")
    void should_create_account_and_return_201_with_account_number() {
        CreateAccountRequest request = new CreateAccountRequest(UUID.randomUUID(), AccountType.SAVINGS);

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/accounts")
        .then()
            .statusCode(201)
            .body("accountNumber", startsWith("ACC-"))
            .body("balance", equalTo(0))
            .body("status", equalTo("ACTIVE"))
            .body("currency", equalTo("USD"))
            .body("type", equalTo("SAVINGS"));
    }

    @Test
    @DisplayName("should_return_404_when_account_not_found")
    void should_return_404_when_account_not_found() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/accounts/" + UUID.randomUUID())
        .then()
            .statusCode(404)
            .body("errorCode", equalTo("ACCOUNT_NOT_FOUND"));
    }

    @Test
    @DisplayName("should_get_account_by_id_when_exists")
    void should_get_account_by_id_when_exists() {
        CreateAccountRequest request = new CreateAccountRequest(UUID.randomUUID(), AccountType.CHECKING);

        String accountId = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/accounts")
        .then()
            .statusCode(201)
            .extract().path("id");

        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/accounts/" + accountId)
        .then()
            .statusCode(200)
            .body("id", equalTo(accountId))
            .body("accountNumber", notNullValue())
            .body("type", equalTo("CHECKING"));
    }

    @Test
    @DisplayName("should_return_400_when_request_missing_required_fields")
    void should_return_400_when_request_missing_required_fields() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/accounts")
        .then()
            .statusCode(400)
            .body("errorCode", equalTo("VALIDATION_ERROR"));
    }
}
