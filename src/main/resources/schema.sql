-- ---------------------------------------------------------------------------
-- StockSense - database schema
--
-- Run once in MySQL Workbench or from the terminal:
--   mysql -u root -p < src/main/resources/schema.sql
--   mysql -u root -p < src/main/resources/sample-data.sql
--
-- The script is safe to run again: it only creates what does not exist yet
-- and never drops a table or deletes data.
-- ---------------------------------------------------------------------------

CREATE DATABASE IF NOT EXISTS stock_recommendation_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE stock_recommendation_db;

-- People who can log in. Passwords are stored as PBKDF2 hashes (PasswordUtil).
CREATE TABLE IF NOT EXISTS users (
    id            INT           NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100)  NOT NULL,
    email         VARCHAR(150)  NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    role          ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role (role)
) ENGINE = InnoDB;

-- Listed companies. api_symbol is the ticker the market data provider expects
-- (for example RELIANCE.BSE on Alpha Vantage). market_cap and the 52-week
-- range stay NULL until a provider has actually supplied them.
CREATE TABLE IF NOT EXISTS stocks (
    id            INT           NOT NULL AUTO_INCREMENT,
    company_name  VARCHAR(150)  NOT NULL,
    symbol        VARCHAR(20)   NOT NULL,
    exchange      VARCHAR(20)   NOT NULL,
    api_symbol    VARCHAR(40)   NOT NULL,
    currency      CHAR(3)       NOT NULL DEFAULT 'USD',
    sector        VARCHAR(80)   NULL,
    industry      VARCHAR(120)  NULL,
    description   TEXT          NULL,
    market_cap    DECIMAL(22,2) NULL,
    week52_high   DECIMAL(19,4) NULL,
    week52_low    DECIMAL(19,4) NULL,
    fundamentals_source     VARCHAR(30) NULL,
    fundamentals_updated_at DATETIME    NULL,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stocks_exchange_symbol (exchange, symbol),
    KEY idx_stocks_company (company_name),
    KEY idx_stocks_sector (sector)
) ENGINE = InnoDB;

-- One end-of-day (or intraday) bar per stock, timestamp and source.
-- data_source is 'SAMPLE' for the demonstration dataset and the provider
-- name (for example 'ALPHA_VANTAGE') for downloaded data, so the two are never mixed.
CREATE TABLE IF NOT EXISTS stock_prices (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    stock_id       INT           NOT NULL,
    open_price     DECIMAL(19,4) NOT NULL,
    high_price     DECIMAL(19,4) NOT NULL,
    low_price      DECIMAL(19,4) NOT NULL,
    close_price    DECIMAL(19,4) NOT NULL,
    adjusted_close DECIMAL(19,4) NULL,
    volume         BIGINT        NULL,
    recorded_at    DATETIME      NOT NULL,
    data_source    VARCHAR(30)   NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_prices_stock_time_source (stock_id, recorded_at, data_source),
    KEY idx_prices_stock_source_time (stock_id, data_source, recorded_at),
    CONSTRAINT fk_prices_stock FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Snapshot of the indicators calculated from stock_prices.
-- sma = SMA(20), sma_long = SMA(50), ema = EMA(20), rsi = RSI(14),
-- macd / signal_line = MACD(12,26,9), volatility = annualised, in percent.
CREATE TABLE IF NOT EXISTS stock_indicators (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    stock_id       INT           NOT NULL,
    sma            DECIMAL(19,4) NULL,
    sma_long       DECIMAL(19,4) NULL,
    ema            DECIMAL(19,4) NULL,
    rsi            DECIMAL(7,3)  NULL,
    macd           DECIMAL(19,4) NULL,
    signal_line    DECIMAL(19,4) NULL,
    volatility     DECIMAL(9,3)  NULL,
    data_source    VARCHAR(30)   NOT NULL,
    data_timestamp DATETIME      NOT NULL,
    calculated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_indicators_stock_time (stock_id, calculated_at),
    CONSTRAINT fk_indicators_stock FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Output of the rule-based engine. INSUFFICIENT_DATA is stored instead of a
-- guess when the price history is too short.
CREATE TABLE IF NOT EXISTS recommendations (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    stock_id             INT           NOT NULL,
    recommendation_type  ENUM('BUY', 'HOLD', 'SELL', 'INSUFFICIENT_DATA') NOT NULL,
    recommendation_score DECIMAL(6,2)  NOT NULL DEFAULT 0,
    explanation          TEXT          NOT NULL,
    supporting_json      TEXT          NULL,
    data_source          VARCHAR(30)   NOT NULL,
    generated_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_timestamp       DATETIME      NULL,
    PRIMARY KEY (id),
    KEY idx_recommendations_stock_time (stock_id, generated_at),
    KEY idx_recommendations_type (recommendation_type),
    CONSTRAINT fk_recommendations_stock FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- A user's private list of followed stocks. One row per user and stock.
CREATE TABLE IF NOT EXISTS watchlists (
    id       INT       NOT NULL AUTO_INCREMENT,
    user_id  INT       NOT NULL,
    stock_id INT       NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_watchlists_user_stock (user_id, stock_id),
    KEY idx_watchlists_stock (stock_id),
    CONSTRAINT fk_watchlists_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
    CONSTRAINT fk_watchlists_stock FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Every call (or skipped call) to the market data provider.
CREATE TABLE IF NOT EXISTS api_sync_logs (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    provider         VARCHAR(30)  NOT NULL,
    symbol           VARCHAR(40)  NOT NULL,
    status           ENUM('SUCCESS', 'CACHED', 'RATE_LIMITED', 'ERROR', 'NO_DATA', 'DEMO') NOT NULL,
    response_message VARCHAR(500) NULL,
    synced_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sync_logs_time (synced_at),
    KEY idx_sync_logs_symbol (symbol)
) ENGINE = InnoDB;

-- Weights and thresholds of the recommendation engine, editable by the admin.
-- A signal contributes +weight (bullish), -weight (bearish) or 0 (neutral).
CREATE TABLE IF NOT EXISTS recommendation_rules (
    id          INT           NOT NULL AUTO_INCREMENT,
    rule_key    VARCHAR(30)   NOT NULL,
    name        VARCHAR(80)   NOT NULL,
    description VARCHAR(400)  NOT NULL,
    weight      DECIMAL(6,2)  NOT NULL DEFAULT 0,
    param_low   DECIMAL(10,4) NULL,
    param_high  DECIMAL(10,4) NULL,
    enabled     BOOLEAN       NOT NULL DEFAULT TRUE,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rules_key (rule_key)
) ENGINE = InnoDB;

-- "Recently viewed" on the dashboard: one row per user and stock.
CREATE TABLE IF NOT EXISTS stock_views (
    id        BIGINT    NOT NULL AUTO_INCREMENT,
    user_id   INT       NOT NULL,
    stock_id  INT       NOT NULL,
    viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_views_user_stock (user_id, stock_id),
    KEY idx_views_user_time (user_id, viewed_at),
    CONSTRAINT fk_views_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
    CONSTRAINT fk_views_stock FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Audit trail shown on the admin dashboard (logins, CRUD actions, syncs).
CREATE TABLE IF NOT EXISTS activity_logs (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    INT          NULL,
    action     VARCHAR(60)  NOT NULL,
    details    VARCHAR(500) NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_activity_time (created_at),
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;
