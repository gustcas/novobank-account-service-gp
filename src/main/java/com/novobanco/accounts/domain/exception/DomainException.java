package com.novobanco.accounts.domain.exception;

/**
 * Sealed hierarchy: el compilador garantiza que ninguna excepción
 * desconocida puede ser lanzada desde el dominio. Cada caso tiene
 * su propio tipo → el manejador de excepciones HTTP puede mapear
 * con precisión sin instanceof genérico.
 */
public sealed interface DomainException
        permits InsufficientFundsException,
                AccountBlockedException,
                AccountClosedException,
                AccountNotFoundException,
                TransactionNotFoundException,
                InvalidAmountException {
}
