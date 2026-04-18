package com.novobanco.accounts.infrastructure.persistence.repository;

import com.novobanco.accounts.infrastructure.persistence.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaAccountRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    /**
     * SELECT FOR UPDATE: bloqueo pesimista a nivel de fila.
     * Serializa el acceso concurrente — el segundo retiro espera
     * a que el primero libere el lock antes de leer el saldo.
     * Esto previene el problema de lost update en concurrencia.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountEntity a WHERE a.id = :id")
    Optional<AccountEntity> findByIdForUpdate(@Param("id") UUID id);
}
