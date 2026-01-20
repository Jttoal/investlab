ALTER TABLE statement_transaction ADD COLUMN account_name TEXT;
CREATE INDEX idx_statement_transaction_account_name ON statement_transaction(account_name);
