package com.novobanco.accounts.domain.port.out;

import com.novobanco.accounts.domain.model.Account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida del dominio. Esta interfaz vive en el dominio.
 * La implementación (JPA) vive en infrastructure/persistence.
 * El dominio no sabe nada de JPA, Hibernate ni Spring Data.
 */
public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(UUID accountId);

    List<Account> findByCustomerId(UUID customerId);

    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Recupera la cuenta con PESSIMISTIC_WRITE lock (SELECT FOR UPDATE).
     * Se usa exclusivamente dentro de transacciones de escritura para
     * serializar el acceso concurrente por fila.
     */
    Optional<Account> findByIdForUpdate(UUID accountId);

    boolean existsByAccountNumber(String accountNumber);
}
