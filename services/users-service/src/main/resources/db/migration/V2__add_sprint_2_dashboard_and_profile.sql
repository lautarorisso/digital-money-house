ALTER TABLE accounts
    ADD COLUMN balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN owner_sub VARCHAR(36) NOT NULL;

CREATE TABLE transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    type VARCHAR(30) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    transaction_date DATETIME(3) NOT NULL,
    balance_after DECIMAL(12,2) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_transactions_account_date_id (account_id, transaction_date DESC, id DESC),
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE users
    ADD COLUMN keycloak_sub VARCHAR(36) NOT NULL;
