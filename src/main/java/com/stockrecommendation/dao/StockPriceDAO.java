package com.stockrecommendation.dao;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.stockrecommendation.model.StockPrice;
import com.stockrecommendation.util.JdbcUtil;

/** SQL for the stock_prices table (the historical OHLCV bars). */
@Repository
public class StockPriceDAO {

    private static final String COLUMNS =
            "id, stock_id, open_price, high_price, low_price, close_price, adjusted_close, volume, recorded_at, data_source";

    private static final RowMapper<StockPrice> MAPPER = (rs, row) -> {
        StockPrice price = new StockPrice();
        price.setId(rs.getLong("id"));
        price.setStockId(rs.getInt("stock_id"));
        price.setOpenPrice(rs.getBigDecimal("open_price"));
        price.setHighPrice(rs.getBigDecimal("high_price"));
        price.setLowPrice(rs.getBigDecimal("low_price"));
        price.setClosePrice(rs.getBigDecimal("close_price"));
        price.setAdjustedClose(rs.getBigDecimal("adjusted_close"));
        price.setVolume(JdbcUtil.getLong(rs, "volume"));
        price.setRecordedAt(JdbcUtil.getDateTime(rs, "recorded_at"));
        price.setDataSource(rs.getString("data_source"));
        return price;
    };

    private final JdbcTemplate jdbc;

    public StockPriceDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The full series of one stock from one source, oldest first. */
    public List<StockPrice> findSeries(int stockId, String dataSource) {
        return jdbc.query("SELECT " + COLUMNS + " FROM stock_prices WHERE stock_id = ? AND data_source = ? ORDER BY recorded_at",
                MAPPER, stockId, dataSource);
    }

    /**
     * The two newest bars of every stock, per source, in one query (ROW_NUMBER needs MySQL 8+).
     *
     * @return stockId -> source -> [newest, previous]
     */
    public Map<Integer, Map<String, List<StockPrice>>> findLatestTwoBars() {
        String sql = "SELECT " + COLUMNS + " FROM ("
                   + "  SELECT p.*, ROW_NUMBER() OVER (PARTITION BY p.stock_id, p.data_source ORDER BY p.recorded_at DESC) AS rn"
                   + "  FROM stock_prices p) ranked WHERE rn <= 2 ORDER BY stock_id, data_source, recorded_at DESC";
        Map<Integer, Map<String, List<StockPrice>>> result = new HashMap<>();
        for (StockPrice price : jdbc.query(sql, MAPPER)) {
            result.computeIfAbsent(price.getStockId(), id -> new LinkedHashMap<>())
                  .computeIfAbsent(price.getDataSource(), source -> new ArrayList<>())
                  .add(price);
        }
        return result;
    }

    /** When this stock's rows from this source were last written (created_at), or null. */
    public LocalDateTime findLastStoredAt(int stockId, String dataSource) {
        Timestamp value = jdbc.queryForObject("SELECT MAX(created_at) FROM stock_prices WHERE stock_id = ? AND data_source = ?",
                Timestamp.class, stockId, dataSource);
        return value == null ? null : value.toLocalDateTime();
    }

    /**
     * Inserts or refreshes a batch of bars in ONE transaction: either the whole
     * download is stored or none of it is.
     */
    @Transactional
    public int upsertAll(int stockId, List<StockPrice> prices) {
        String sql = "INSERT INTO stock_prices (stock_id, open_price, high_price, low_price, close_price, adjusted_close, "
                   + "volume, recorded_at, data_source) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE open_price = VALUES(open_price), high_price = VALUES(high_price), "
                   + "low_price = VALUES(low_price), close_price = VALUES(close_price), "
                   + "adjusted_close = VALUES(adjusted_close), volume = VALUES(volume), created_at = CURRENT_TIMESTAMP";
        List<Object[]> rows = new ArrayList<>();
        for (StockPrice price : prices) {
            rows.add(new Object[] {stockId, price.getOpenPrice(), price.getHighPrice(), price.getLowPrice(),
                    price.getClosePrice(), price.getAdjustedClose(), price.getVolume(),
                    JdbcUtil.timestamp(price.getRecordedAt()), price.getDataSource()});
        }
        int changed = 0;
        for (int count : jdbc.batchUpdate(sql, rows)) {
            changed += Math.max(count, 0);
        }
        return changed;
    }

    public long countAll() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM stock_prices", Long.class);
    }

    /** One row per source: [source, rows, stocks, newest bar, last written]. */
    public List<Object[]> summariseBySource() {
        return jdbc.query("SELECT data_source, COUNT(*) AS row_count, COUNT(DISTINCT stock_id) AS stock_count, "
                + "MAX(recorded_at) AS newest_bar, MAX(created_at) AS stored_at FROM stock_prices "
                + "GROUP BY data_source ORDER BY data_source",
                (rs, row) -> new Object[] {rs.getString("data_source"), rs.getLong("row_count"), rs.getInt("stock_count"),
                        JdbcUtil.getDateTime(rs, "newest_bar"), JdbcUtil.getDateTime(rs, "stored_at")});
    }
}
