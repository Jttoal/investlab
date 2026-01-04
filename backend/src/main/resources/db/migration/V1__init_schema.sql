-- Account table
CREATE TABLE account (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    broker VARCHAR(100) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'CNY',
    balance_manual DECIMAL(15,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_account_name ON account(name);

-- Strategy table
CREATE TABLE strategy (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    goal_note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_strategy_name ON strategy(name);
CREATE INDEX idx_strategy_type ON strategy(type);

-- Asset table
CREATE TABLE asset (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    symbol VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    market VARCHAR(10) NOT NULL,
    strategy_id INTEGER NOT NULL,
    target_low DECIMAL(10,2),
    target_high DECIMAL(10,2),
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (strategy_id) REFERENCES strategy(id) ON DELETE CASCADE
);

CREATE INDEX idx_asset_symbol ON asset(symbol);
CREATE INDEX idx_asset_strategy ON asset(strategy_id);

-- Trade table
CREATE TABLE trade (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    strategy_id INTEGER NOT NULL,
    account_id INTEGER NOT NULL,
    asset_id INTEGER NOT NULL,
    type VARCHAR(20) NOT NULL,
    price DECIMAL(10,4) NOT NULL,
    quantity INTEGER NOT NULL,
    fee DECIMAL(10,2) NOT NULL DEFAULT 0,
    trade_date DATE NOT NULL,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (strategy_id) REFERENCES strategy(id) ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE CASCADE,
    FOREIGN KEY (asset_id) REFERENCES asset(id) ON DELETE CASCADE
);

CREATE INDEX idx_trade_strategy ON trade(strategy_id);
CREATE INDEX idx_trade_account ON trade(account_id);
CREATE INDEX idx_trade_asset ON trade(asset_id);
CREATE INDEX idx_trade_date ON trade(trade_date);
CREATE INDEX idx_trade_type ON trade(type);

-- Viewpoint table
CREATE TABLE viewpoint (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    strategy_id INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary TEXT,
    link VARCHAR(500),
    tag VARCHAR(50),
    viewpoint_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (strategy_id) REFERENCES strategy(id) ON DELETE CASCADE
);

CREATE INDEX idx_viewpoint_strategy ON viewpoint(strategy_id);
CREATE INDEX idx_viewpoint_date ON viewpoint(viewpoint_date);
CREATE INDEX idx_viewpoint_tag ON viewpoint(tag);
