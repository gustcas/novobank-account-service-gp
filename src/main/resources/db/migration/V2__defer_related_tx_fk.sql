-- La FK auto-referencial related_tx_id se verifica al INSERT del débito,
-- pero el crédito aún no existe → violación de FK antes del COMMIT.
-- DEFERRABLE INITIALLY DEFERRED mueve la verificación al final de la
-- transacción, cuando ambas filas (débito y crédito) ya están presentes.
ALTER TABLE transactions
    DROP CONSTRAINT fk_transactions_related_tx;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_related_tx
        FOREIGN KEY (related_tx_id) REFERENCES transactions(id)
        DEFERRABLE INITIALLY DEFERRED;
