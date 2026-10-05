CREATE TABLE cards (
    id BIGINT NOT NULL AUTO_INCREMENT,
    card_number VARCHAR(19) NOT NULL,
    type VARCHAR(10) NOT NULL,
    account_id BIGINT DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cards_card_number (card_number),
    KEY idx_cards_account_id (account_id),
    CONSTRAINT fk_card_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
