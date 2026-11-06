package com.stockrecommendation.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.stockrecommendation.model.StockIndicator;
import com.stockrecommendation.util.JdbcUtil;

/** SQL for the stock_indicators table (snapshots of calculated indicators). */
@Repository
public class IndicatorDAO {

    private final JdbcTemplate jdbc;

    public IndicatorDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(StockIndicator indicator) {
        jdbc.update("INSERT INTO stock_indicators (stock_id, sma, sma_long, ema, rsi, macd, signal_line, volatility, "
                + "data_source, data_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                indicator.getStockId(), indicator.getSma(), indicator.getSmaLong(), indicator.getEma(), indicator.getRsi(),
                indicator.getMacd(), indicator.getSignalLine(), indicator.getVolatility(), indicator.getDataSource(),
                JdbcUtil.timestamp(indicator.getDataTimestamp()));
    }

    /** Has a snapshot already been stored for exactly this data (stock, source, newest bar)? */
    public boolean exists(int stockId, String dataSource, java.time.LocalDateTime dataTimestamp) {
        return !jdbc.queryForList("SELECT 1 FROM stock_indicators WHERE stock_id = ? AND data_source = ? AND data_timestamp = ? LIMIT 1",
                Integer.class, stockId, dataSource, JdbcUtil.timestamp(dataTimestamp)).isEmpty();
    }
}
