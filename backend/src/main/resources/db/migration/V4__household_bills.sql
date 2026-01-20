CREATE TABLE statement_file (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL DEFAULT 0,
    file_name VARCHAR(255) NOT NULL,
    file_md5 VARCHAR(64),
    file_path TEXT,
    period_start DATE,
    period_end DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    total_rows INTEGER NOT NULL DEFAULT 0,
    inserted_rows INTEGER NOT NULL DEFAULT 0,
    dedup_rows INTEGER NOT NULL DEFAULT 0,
    failed_rows INTEGER NOT NULL DEFAULT 0,
    error_msg TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP
);

CREATE INDEX idx_statement_file_status ON statement_file(status);
CREATE INDEX idx_statement_file_created_at ON statement_file(created_at);

CREATE TABLE statement_transaction (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL DEFAULT 0,
    file_id INTEGER NOT NULL,
    txn_date DATE NOT NULL,
    currency VARCHAR(10) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    balance DECIMAL(15,2),
    txn_type_raw TEXT,
    counterparty TEXT,
    category VARCHAR(20) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    dedup_key VARCHAR(120) NOT NULL,
    hash_raw TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES statement_file(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_statement_transaction_dedup ON statement_transaction(dedup_key);
CREATE INDEX idx_statement_transaction_date ON statement_transaction(txn_date);
CREATE INDEX idx_statement_transaction_category_direction ON statement_transaction(category, direction);

-- 初始化投资类关键词配置
INSERT INTO setting (config_key, config_value, value_type, description, created_at, updated_at)
VALUES (
    'household_bills.invest_category',
    '{
  "billing_summary_keyword": [
    "受托理财申购",
    "受托理财赎回",
    "基金定期定额申购",
    "基金申购",
    "申购",
    "基金赎回",
    "朝朝宝转入",
    "朝朝宝自动转入",
    "朝朝宝转出",
    "基金认购",
    "银证转账(第三方存管)",
    "受托理财分红"
  ],
  "counter_party_keyword": [
    "盈米基金",
    "蚂蚁基金",
    "广发基金",
    "景顺长城基金",
    "基金销售"
  ]
}',
    'json',
    '家庭账单投资类交易分类规则：包含交易摘要关键词和对手信息关键词',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
