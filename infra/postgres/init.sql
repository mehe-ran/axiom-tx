CREATE TABLE IF NOT EXISTS accounts (
    account_id VARCHAR(50) PRIMARY KEY,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id VARCHAR(50) PRIMARY KEY,
    sender_id VARCHAR(50) REFERENCES accounts(account_id),
    receiver_id VARCHAR(50) REFERENCES accounts(account_id),
    amount DECIMAL(15, 2) NOT NULL,
    status VARCHAR(20) DEFAULT 'processed'
);