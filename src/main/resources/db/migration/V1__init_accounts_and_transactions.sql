-- ============================================================
-- NovoBanco — Esquema inicial de cuentas y transacciones
-- V1: Creación de tablas, constraints, índices
-- ============================================================

-- --------------------------------------------------------
-- TABLA: accounts
-- Justificación de tipos:
--   id UUID: generación distribuida sin secuencia central
--   balance NUMERIC(19,4): exactitud decimal mandatoria en banca
--     (IEEE 754 double produce errores de redondeo inaceptables)
--   CHECK(balance >= 0): segunda defensa contra saldo negativo;
--     la primera es la capa de aplicación, pero la DB es el
--     último guardián si alguna ruta de código la omite
-- --------------------------------------------------------
CREATE TABLE accounts (
    id             UUID            NOT NULL,
    customer_id    UUID            NOT NULL,
    account_number VARCHAR(20)     NOT NULL,
    type           VARCHAR(20)     NOT NULL,
    currency       VARCHAR(3)      NOT NULL DEFAULT 'USD',
    balance        NUMERIC(19, 4)  NOT NULL DEFAULT 0,
    status         VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uq_accounts_account_number UNIQUE (account_number),
    CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_accounts_type CHECK (type IN ('SAVINGS', 'CHECKING')),
    CONSTRAINT chk_accounts_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),
    CONSTRAINT chk_accounts_currency CHECK (currency IN ('USD'))
);

-- --------------------------------------------------------
-- TABLA: transactions
-- Justificación de diseño:
--   related_tx_id (FK auto-referencial): vincula las dos
--     mitades de una transferencia (débito ↔ crédito)
--     sin necesidad de una tabla separada transfer_ledger.
--     Normalización vs. rendimiento: mantener ambas partes
--     en la misma tabla simplifica la consulta de historial
--     y evita JOINs para el caso de uso más frecuente.
--   idempotency_key UNIQUE: el constraint a nivel DB garantiza
--     que incluso con race conditions no se procese dos veces
--     el mismo request. La aplicación verifica primero, la DB
--     es la última defensa.
--   reference UUID UNIQUE: identificador externo trazable
--     que se puede compartir con el cliente para soporte.
-- --------------------------------------------------------
CREATE TABLE transactions (
    id               UUID            NOT NULL,
    account_id       UUID            NOT NULL,
    type             VARCHAR(20)     NOT NULL,
    amount           NUMERIC(19, 4)  NOT NULL,
    status           VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS',
    reference        UUID            NOT NULL,
    related_tx_id    UUID,
    idempotency_key  UUID,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT uq_transactions_reference UNIQUE (reference),
    CONSTRAINT uq_transactions_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transactions_related_tx
        FOREIGN KEY (related_tx_id) REFERENCES transactions(id),
    CONSTRAINT chk_transactions_type
        CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_DEBIT', 'TRANSFER_CREDIT')),
    CONSTRAINT chk_transactions_status
        CHECK (status IN ('SUCCESS', 'FAILED', 'REVERSED')),
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0)
);

-- ============================================================
-- ÍNDICES — justificación de cada uno:
-- ============================================================

-- Consulta crítica: historial paginado de una cuenta ordenado DESC
-- "SELECT * FROM transactions WHERE account_id = ? ORDER BY created_at DESC LIMIT 20"
-- Sin este índice: Seq Scan + Sort en toda la tabla → O(n log n)
-- Con este índice: Index Scan directo → O(log n + k) donde k = página
CREATE INDEX idx_transactions_account_created
    ON transactions (account_id, created_at DESC);

-- Verificación de idempotencia: "¿existe esta idempotency_key?"
-- El UNIQUE constraint ya crea un índice implícito, pero lo nombramos
-- explícitamente para claridad en los query plans
-- (el UNIQUE constraint ya lo maneja, este índice es redundante — ver nota)
-- NOTA: el UNIQUE constraint sobre idempotency_key ya provee el índice.
-- Lo omitimos para no duplicar; el query planner usará el constraint index.

-- Búsqueda por referencia externa (soporte al cliente, trazabilidad)
-- "SELECT * FROM transactions WHERE reference = ?"
-- El UNIQUE constraint ya crea el índice implícito sobre reference.

-- Transferencias salientes de un cliente en los últimos 30 días:
-- "SELECT COUNT(*) FROM transactions t JOIN accounts a ON t.account_id = a.id
--  WHERE a.customer_id = ? AND t.type = 'TRANSFER_DEBIT'
--  AND t.created_at > NOW() - INTERVAL '30 days'"
CREATE INDEX idx_transactions_type_created
    ON transactions (type, created_at DESC);

CREATE INDEX idx_accounts_customer_id
    ON accounts (customer_id);

-- ============================================================
-- COMENTARIOS DE ESQUEMA (visible en psql \d+)
-- ============================================================
COMMENT ON TABLE accounts IS 'Cuentas bancarias de clientes NovoBanco';
COMMENT ON TABLE transactions IS 'Registro inmutable de todas las operaciones financieras';
COMMENT ON COLUMN accounts.balance IS 'Saldo disponible en USD con precision NUMERIC(19,4)';
COMMENT ON COLUMN transactions.related_tx_id IS 'Vincula debito y credito en una transferencia atomica';
COMMENT ON COLUMN transactions.idempotency_key IS 'Clave de idempotencia para detectar requests duplicados';
