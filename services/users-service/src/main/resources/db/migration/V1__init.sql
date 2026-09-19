CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    apellido VARCHAR(30) NOT NULL,
    dni BIGINT NOT NULL,
    email VARCHAR(254) NOT NULL,
    nombre VARCHAR(30) NOT NULL,
    telefono VARCHAR(30) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    alias VARCHAR(60) NOT NULL,
    cvu VARCHAR(22) NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_accounts_alias (alias),
    UNIQUE KEY uk_accounts_cvu (cvu),
    UNIQUE KEY uk_accounts_user_id (user_id),
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rol (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rol_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_rol (
    user_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    CONSTRAINT fk_user_rol_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_rol_rol FOREIGN KEY (rol_id) REFERENCES rol (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;