package com.novobanco.accounts.infrastructure.persistence.repository;

import com.novobanco.accounts.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaTransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    Optional<TransactionEntity> findByReference(UUID reference);

    Optional<TransactionEntity> findByIdempotencyKey(UUID idempotencyKey);

    /**
     * Historial paginado ordenado DESC.
     * El índice (account_id, created_at DESC) hace que esta consulta
     * sea un Index Scan directo — O(log n + k) independiente del total
     * de filas de la tabla.
     */
    @Query("SELECT t FROM TransactionEntity t WHERE t.account.id = :accountId " +
           "ORDER BY t.createdAt DESC")
    Page<TransactionEntity> findByAccountId(@Param("accountId") UUID accountId, Pageable pageable);
}
