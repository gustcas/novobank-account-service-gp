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

/*
 * DIAGRAMA ER (ASCII)
 *
 *  accounts                          transactions
 * ┌──────────────────┐              ┌─────────────────────────┐
 * │ id (UUID) PK     │◄─────────────│ account_id (UUID) FK    │
 * │ customer_id UUID │              │ id (UUID) PK            │
 * │ account_number   │              │ type VARCHAR(20)        │
 * │ type VARCHAR(20) │              │ amount NUMERIC(19,4)    │
 * │ currency (USD)   │              │ status VARCHAR(20)      │
 * │ balance NUM(19,4)│              │ reference UUID UNIQUE   │
 * │ status VARCHAR   │              │ related_tx_id UUID FK──┐│
 * │ created_at TZ    │              │ idempotency_key UUID   ││
 * └──────────────────┘              │ created_at TIMESTAMPTZ ││
 *                                   └────────────────────────┘│
 *                                        ▲ (self-referential)  │
 *                                        └─────────────────────┘
 *
 * related_tx_id vincula las dos mitades de una transferencia:
 *   TRANSFER_DEBIT (cuenta origen) ↔ TRANSFER_CREDIT (cuenta destino)
 */
