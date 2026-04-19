-- ============================================================
-- NovoBanco — schema.sql (standalone, ejecutable directamente)
-- Este archivo es una copia de la migración Flyway V1.
-- Fuente de verdad: src/main/resources/db/migration/V1__init_accounts_and_transactions.sql
--
-- Ejecutar: psql -U novobanco -d novobanco -f schema.sql
-- ============================================================

-- --------------------------------------------------------
-- TABLA: accounts
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
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
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
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

-- --------------------------------------------------------
-- ÍNDICES
-- --------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_transactions_account_created
    ON transactions (account_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_type_created
    ON transactions (type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_accounts_customer_id
    ON accounts (customer_id);

-- --------------------------------------------------------
-- COMENTARIOS
-- --------------------------------------------------------
COMMENT ON TABLE accounts IS 'Cuentas bancarias de clientes NovoBanco';
COMMENT ON TABLE transactions IS 'Registro inmutable de todas las operaciones financieras';
COMMENT ON COLUMN accounts.balance IS 'Saldo disponible en USD con precision NUMERIC(19,4)';
COMMENT ON COLUMN transactions.related_tx_id IS 'Vincula debito y credito en una transferencia atomica';
COMMENT ON COLUMN transactions.idempotency_key IS 'Clave de idempotencia para detectar requests duplicados';

-- ============================================================
-- DIAGRAMA ENTIDAD-RELACIÓN (ASCII)
-- ============================================================
--
--   CLIENTE (externo)
--   customer_id UUID ──────────────────────────────────────────┐
--                                                              │ 1
--                                                              ▼
--  ┌─────────────────────────────────────────────────┐        N
--  │                    ACCOUNTS                     │ ◄──────┘
--  ├─────────────────────────────────────────────────┤
--  │ PK  id             UUID          NOT NULL        │
--  │     customer_id    UUID          NOT NULL        │  ← referencia externa
--  │ UQ  account_number VARCHAR(20)   NOT NULL        │
--  │     type           VARCHAR(20)   SAVINGS|CHECKING│
--  │     currency       VARCHAR(3)    DEFAULT 'USD'   │
--  │     balance        NUMERIC(19,4) DEFAULT 0       │  ← CHECK >= 0
--  │     status         VARCHAR(20)   ACTIVE|BLOCKED  │
--  │     created_at     TIMESTAMPTZ   NOT NULL        │
--  └────────────────────────┬────────────────────────┘
--                           │ 1
--                           │ fk_transactions_account
--                           │ N
--  ┌────────────────────────▼────────────────────────┐
--  │                  TRANSACTIONS                   │
--  ├─────────────────────────────────────────────────┤
--  │ PK  id              UUID          NOT NULL       │ ◄──┐
--  │ FK  account_id      UUID          NOT NULL       │    │ fk_transactions_related_tx
--  │     type            VARCHAR(20)                  │    │ DEFERRABLE INITIALLY DEFERRED
--  │     amount          NUMERIC(19,4) CHECK > 0      │    │
--  │     status          VARCHAR(20)   SUCCESS|FAILED │    │
--  │ UQ  reference       UUID          NOT NULL       │    │
--  │ FK? related_tx_id   UUID          NULLABLE       │────┘ (auto-referencial: débito ↔ crédito)
--  │ UQ  idempotency_key UUID          NULLABLE       │
--  │     created_at      TIMESTAMPTZ   NOT NULL       │
--  └─────────────────────────────────────────────────┘
--
-- CARDINALIDADES:
--   accounts    1 ──── N    transactions   (una cuenta tiene muchas transacciones)
--   transactions 1 ──── 1   transactions   (débito vinculado a crédito via related_tx_id)
--
-- ÍNDICES y CONSULTAS QUE SOPORTAN:
--
--   idx_transactions_account_created  (account_id, created_at DESC)
--     → Q2: últimos 20 movimientos de cuenta X ordenados por fecha DESC
--
--   idx_transactions_type_created     (type, created_at DESC)
--     → Q3: transferencias salientes del cliente Y en los últimos 30 días
--
--   idx_accounts_customer_id          (customer_id)
--     → todas las cuentas de un cliente (JOIN con transactions para Q3)
--
--   UNIQUE(reference)                 — índice implícito del constraint
--     → Q4: ¿existe transacción con referencia Z? (detección de duplicados)
--
--   UNIQUE(account_number)            — índice implícito del constraint
--     → Q1: saldo actual de cuenta X via GET /accounts/{id}
--
-- RESPUESTAS A LAS 4 CONSULTAS DEL ENUNCIADO:
--
--   Q1. SELECT balance FROM accounts WHERE id = ?
--       → O(log n) via PK index
--
--   Q2. SELECT * FROM transactions
--       WHERE account_id = ? ORDER BY created_at DESC LIMIT 20
--       → O(log n + 20) via idx_transactions_account_created
--
--   Q3. SELECT COUNT(*) FROM transactions t
--       JOIN accounts a ON t.account_id = a.id
--       WHERE a.customer_id = ? AND t.type = 'TRANSFER_DEBIT'
--         AND t.created_at > NOW() - INTERVAL '30 days'
--       → O(log n) via idx_accounts_customer_id + idx_transactions_type_created
--
--   Q4. SELECT id FROM transactions WHERE reference = ?
--       → O(log n) via UNIQUE index en reference
--
