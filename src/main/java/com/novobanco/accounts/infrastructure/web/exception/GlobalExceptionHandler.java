package com.novobanco.accounts.infrastructure.web.exception;

import com.novobanco.accounts.domain.exception.AccountBlockedException;
import com.novobanco.accounts.domain.exception.AccountClosedException;
import com.novobanco.accounts.domain.exception.AccountNotFoundException;
import com.novobanco.accounts.domain.exception.InsufficientFundsException;
import com.novobanco.accounts.domain.exception.InvalidAmountException;
import com.novobanco.accounts.domain.exception.TransactionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones. Implementa RFC 7807 Problem Details.
 * Regla: nunca exponer stack trace. Nunca retornar 400 genérico para
 * violaciones de reglas de negocio — siempre usar 422 con código específico.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_URI = "https://novobanco.com/errors/";

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleInsufficientFunds(InsufficientFundsException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setType(URI.create(BASE_URI + "fondos-insuficientes"));
        problem.setTitle("Fondos Insuficientes");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", "INSUFFICIENT_FUNDS");
        problem.setProperty("accountNumber", ex.getAccountNumber());
        problem.setProperty("currentBalance", ex.getCurrentBalance());
        problem.setProperty("requestedAmount", ex.getRequestedAmount());
        return problem;
    }

    @ExceptionHandler(AccountBlockedException.class)
    public ProblemDetail handleAccountBlocked(AccountBlockedException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setType(URI.create(BASE_URI + "cuenta-bloqueada"));
        problem.setTitle("Cuenta Bloqueada");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", "ACCOUNT_BLOCKED");
        problem.setProperty("accountNumber", ex.getAccountNumber());
        return problem;
    }

    @ExceptionHandler(AccountClosedException.class)
    public ProblemDetail handleAccountClosed(AccountClosedException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setType(URI.create(BASE_URI + "cuenta-cerrada"));
        problem.setTitle("Cuenta Cerrada");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", "ACCOUNT_CLOSED");
        problem.setProperty("accountNumber", ex.getAccountNumber());
        return problem;
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create(BASE_URI + "cuenta-no-encontrada"));
        problem.setTitle("Cuenta No Encontrada");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", "ACCOUNT_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ProblemDetail handleTransactionNotFound(TransactionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create(BASE_URI + "transaccion-no-encontrada"));
        problem.setTitle("Transacción No Encontrada");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", "TRANSACTION_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ProblemDetail handleInvalidAmount(InvalidAmountException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(BASE_URI + "monto-invalido"));
        problem.setTitle("Monto Inválido");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", "INVALID_AMOUNT");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a
                ));

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(BASE_URI + "validacion-fallida"));
        problem.setTitle("Validación Fallida");
        problem.setDetail("One or more fields are invalid");
        problem.setProperty("errorCode", "VALIDATION_ERROR");
        problem.setProperty("fieldErrors", errors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setType(URI.create(BASE_URI + "error-interno"));
        problem.setTitle("Error Interno del Servidor");
        problem.setDetail("An unexpected error occurred. Please contact support.");
        problem.setProperty("errorCode", "INTERNAL_ERROR");
        return problem;
    }
}
