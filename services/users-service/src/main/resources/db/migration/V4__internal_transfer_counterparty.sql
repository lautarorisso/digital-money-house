ALTER TABLE transactions
    ADD COLUMN counterparty_account_id BIGINT DEFAULT NULL,
    ADD KEY idx_transactions_outbound_recipient (account_id, type, counterparty_account_id, id),
    ADD CONSTRAINT fk_transaction_counterparty
        FOREIGN KEY (counterparty_account_id) REFERENCES accounts (id);
