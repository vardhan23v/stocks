package com.stockrecommendation.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import com.stockrecommendation.model.StockIndicator;
import com.stockrecommendation.util.DBConnection;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.JdbcUtil;

/** SQL for the stock_indicators table (snapshots of calculated indicators). */
public class IndicatorDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    public void insert(StockIndicator indicator) {
        String sql = "INSERT INTO stock_indicators (stock_id, sma, sma_long, ema, rsi, macd, signal_line, volatility, "
                   + "data_source, data_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, indicator.getStockId());
            JdbcUtil.setDecimal(statement, 2, indicator.getSma());
            JdbcUtil.setDecimal(statement, 3, indicator.getSmaLong());
            JdbcUtil.setDecimal(statement, 4, indicator.getEma());
            JdbcUtil.setDecimal(statement, 5, indicator.getRsi());
            JdbcUtil.setDecimal(statement, 6, indicator.getMacd());
            JdbcUtil.setDecimal(statement, 7, indicator.getSignalLine());
            JdbcUtil.setDecimal(statement, 8, indicator.getVolatility());
            statement.setString(9, indicator.getDataSource());
            JdbcUtil.setDateTime(statement, 10, indicator.getDataTimestamp());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the indicators.", e);
        }
    }

    /** Has a snapshot already been stored for exactly this data (stock, source, newest bar)? */
    public boolean exists(int stockId, String dataSource, LocalDateTime dataTimestamp) {
        String sql = "SELECT 1 FROM stock_indicators WHERE stock_id = ? AND data_source = ? AND data_timestamp = ? LIMIT 1";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stockId);
            statement.setString(2, dataSource);
            JdbcUtil.setDateTime(statement, 3, dataTimestamp);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the indicators.", e);
        }
    }

    /** The newest snapshot of one stock, or null. */
    public StockIndicator findLatest(int stockId) {
        String sql = "SELECT id, stock_id, sma, sma_long, ema, rsi, macd, signal_line, volatility, data_source, "
                   + "data_timestamp, calculated_at FROM stock_indicators WHERE stock_id = ? "
                   + "ORDER BY calculated_at DESC, id DESC LIMIT 1";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stockId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                StockIndicator indicator = new StockIndicator();
                indicator.setId(resultSet.getLong("id"));
                indicator.setStockId(resultSet.getInt("stock_id"));
                indicator.setSma(resultSet.getBigDecimal("sma"));
                indicator.setSmaLong(resultSet.getBigDecimal("sma_long"));
                indicator.setEma(resultSet.getBigDecimal("ema"));
                indicator.setRsi(resultSet.getBigDecimal("rsi"));
                indicator.setMacd(resultSet.getBigDecimal("macd"));
                indicator.setSignalLine(resultSet.getBigDecimal("signal_line"));
                indicator.setVolatility(resultSet.getBigDecimal("volatility"));
                indicator.setDataSource(resultSet.getString("data_source"));
                indicator.setDataTimestamp(JdbcUtil.getDateTime(resultSet, "data_timestamp"));
                indicator.setCalculatedAt(JdbcUtil.getDateTime(resultSet, "calculated_at"));
                return indicator;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the indicators.", e);
        }
    }
}
