package com.stockrecommendation.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.stockrecommendation.model.StockPrice;
import com.stockrecommendation.util.DBConnection;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.JdbcUtil;

/** SQL for the stock_prices table (the historical OHLCV bars). */
public class StockPriceDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String COLUMNS =
            "id, stock_id, open_price, high_price, low_price, close_price, adjusted_close, volume, recorded_at, data_source";

    /** The full series of one stock from one source, oldest first. */
    public List<StockPrice> findSeries(int stockId, String dataSource) {
        String sql = "SELECT " + COLUMNS + " FROM stock_prices WHERE stock_id = ? AND data_source = ? ORDER BY recorded_at";
        List<StockPrice> prices = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stockId);
            statement.setString(2, dataSource);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    prices.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the price history.", e);
        }
        return prices;
    }

    /**
     * The two newest bars of every stock, per source, in one query
     * (ROW_NUMBER needs MySQL 8+).
     *
     * @return stockId -> source -> [newest, previous]
     */
    public Map<Integer, Map<String, List<StockPrice>>> findLatestTwoBars() {
        String sql = "SELECT " + COLUMNS + " FROM ("
                   + "  SELECT p.*, ROW_NUMBER() OVER (PARTITION BY p.stock_id, p.data_source ORDER BY p.recorded_at DESC) AS rn"
                   + "  FROM stock_prices p) ranked WHERE rn <= 2 ORDER BY stock_id, data_source, recorded_at DESC";
        Map<Integer, Map<String, List<StockPrice>>> result = new HashMap<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                StockPrice price = mapRow(resultSet);
                result.computeIfAbsent(price.getStockId(), id -> new LinkedHashMap<>())
                      .computeIfAbsent(price.getDataSource(), source -> new ArrayList<>())
                      .add(price);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the latest prices.", e);
        }
        return result;
    }

    /** Newest bar timestamp of one stock and source, or null when there is none. */
    public LocalDateTime findLatestTimestamp(int stockId, String dataSource) {
        String sql = "SELECT MAX(recorded_at) FROM stock_prices WHERE stock_id = ? AND data_source = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stockId);
            statement.setString(2, dataSource);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                java.sql.Timestamp value = resultSet.getTimestamp(1);
                return value == null ? null : value.toLocalDateTime();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the price timestamp.", e);
        }
    }

    /** When this stock's rows from this source were last written (created_at), or null. */
    public LocalDateTime findLastStoredAt(int stockId, String dataSource) {
        String sql = "SELECT MAX(created_at) FROM stock_prices WHERE stock_id = ? AND data_source = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stockId);
            statement.setString(2, dataSource);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                java.sql.Timestamp value = resultSet.getTimestamp(1);
                return value == null ? null : value.toLocalDateTime();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the cache timestamp.", e);
        }
    }

    /**
     * Inserts or refreshes a batch of bars in ONE transaction: either the
     * whole download is stored or none of it is.
     *
     * @return number of rows inserted or changed
     */
    public int upsertAll(int stockId, List<StockPrice> prices) {
        String sql = "INSERT INTO stock_prices (stock_id, open_price, high_price, low_price, close_price, adjusted_close, "
                   + "volume, recorded_at, data_source) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE open_price = VALUES(open_price), high_price = VALUES(high_price), "
                   + "low_price = VALUES(low_price), close_price = VALUES(close_price), "
                   + "adjusted_close = VALUES(adjusted_close), volume = VALUES(volume), created_at = CURRENT_TIMESTAMP";
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (StockPrice price : prices) {
                    statement.setInt(1, stockId);
                    statement.setBigDecimal(2, price.getOpenPrice());
                    statement.setBigDecimal(3, price.getHighPrice());
                    statement.setBigDecimal(4, price.getLowPrice());
                    statement.setBigDecimal(5, price.getClosePrice());
                    JdbcUtil.setDecimal(statement, 6, price.getAdjustedClose());
                    JdbcUtil.setLong(statement, 7, price.getVolume());
                    JdbcUtil.setDateTime(statement, 8, price.getRecordedAt());
                    statement.setString(9, price.getDataSource());
                    statement.addBatch();
                }
                int changed = 0;
                for (int count : statement.executeBatch()) {
                    changed += Math.max(count, 0);
                }
                connection.commit();
                return changed;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not store the downloaded prices.", e);
        }
    }

    public long countAll() {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM stock_prices");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException e) {
            throw new DataAccessException("Could not count the price rows.", e);
        }
    }

    /** Row count and newest bar per source - for the market data page. */
    public List<Object[]> summariseBySource() {
        String sql = "SELECT data_source, COUNT(*), COUNT(DISTINCT stock_id), MAX(recorded_at) AS newest_bar, MAX(created_at) AS stored_at "
                   + "FROM stock_prices GROUP BY data_source ORDER BY data_source";
        List<Object[]> rows = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rows.add(new Object[] {
                    resultSet.getString(1), resultSet.getLong(2), resultSet.getInt(3),
                    JdbcUtil.getDateTime(resultSet, "newest_bar"), JdbcUtil.getDateTime(resultSet, "stored_at")
                });
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not summarise the price data.", e);
        }
        return rows;
    }

    private StockPrice mapRow(ResultSet resultSet) throws SQLException {
        StockPrice price = new StockPrice();
        price.setId(resultSet.getLong("id"));
        price.setStockId(resultSet.getInt("stock_id"));
        price.setOpenPrice(resultSet.getBigDecimal("open_price"));
        price.setHighPrice(resultSet.getBigDecimal("high_price"));
        price.setLowPrice(resultSet.getBigDecimal("low_price"));
        price.setClosePrice(resultSet.getBigDecimal("close_price"));
        price.setAdjustedClose(resultSet.getBigDecimal("adjusted_close"));
        price.setVolume(JdbcUtil.getLong(resultSet, "volume"));
        price.setRecordedAt(JdbcUtil.getDateTime(resultSet, "recorded_at"));
        price.setDataSource(resultSet.getString("data_source"));
        return price;
    }
}
