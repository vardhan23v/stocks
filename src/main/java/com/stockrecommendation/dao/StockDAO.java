package com.stockrecommendation.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.stockrecommendation.model.Stock;
import com.stockrecommendation.util.DBConnection;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.JdbcUtil;

/**
 * SQL for the stocks table and the small stock_views table ("recently viewed").
 * Quotes are not loaded here - StockService attaches them from StockPriceDAO.
 */
public class StockDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String SELECT_STOCKS =
            "SELECT s.id, s.company_name, s.symbol, s.exchange, s.api_symbol, s.currency, s.sector, s.industry, "
          + "s.description, s.market_cap, s.week52_high, s.week52_low, s.fundamentals_source, "
          + "s.fundamentals_updated_at, s.is_active, s.created_at, s.updated_at FROM stocks s ";

    /**
     * Searches by company name or ticker, optionally restricted to one sector
     * and/or exchange. Empty filters are ignored.
     */
    public List<Stock> search(String text, String sector, String exchange, boolean includeInactive) {
        StringBuilder sql = new StringBuilder(SELECT_STOCKS).append("WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        if (!includeInactive) {
            sql.append(" AND s.is_active = TRUE");
        }
        if (text != null && !text.isBlank()) {
            sql.append(" AND (s.company_name LIKE ? OR s.symbol LIKE ?)");
            String pattern = JdbcUtil.likePattern(text.trim());
            parameters.add(pattern);
            parameters.add(pattern);
        }
        if (sector != null && !sector.isBlank()) {
            sql.append(" AND s.sector = ?");
            parameters.add(sector);
        }
        if (exchange != null && !exchange.isBlank()) {
            sql.append(" AND s.exchange = ?");
            parameters.add(exchange);
        }
        sql.append(" ORDER BY s.symbol");
        return query(sql.toString(), parameters.toArray());
    }

    public List<Stock> findAllActive() {
        return query(SELECT_STOCKS + "WHERE s.is_active = TRUE ORDER BY s.symbol");
    }

    public Stock findById(int stockId) {
        List<Stock> stocks = query(SELECT_STOCKS + "WHERE s.id = ?", stockId);
        return stocks.isEmpty() ? null : stocks.get(0);
    }

    public Stock findByExchangeAndSymbol(String exchange, String symbol) {
        List<Stock> stocks = query(SELECT_STOCKS + "WHERE s.exchange = ? AND s.symbol = ?", exchange, symbol);
        return stocks.isEmpty() ? null : stocks.get(0);
    }

    public List<String> findSectors() {
        return strings("SELECT DISTINCT sector FROM stocks WHERE sector IS NOT NULL AND is_active = TRUE ORDER BY sector");
    }

    public List<String> findExchanges() {
        return strings("SELECT DISTINCT exchange FROM stocks WHERE is_active = TRUE ORDER BY exchange");
    }

    /** Inserts a stock and stores its generated id. */
    public void insert(Stock stock) {
        String sql = "INSERT INTO stocks (company_name, symbol, exchange, api_symbol, currency, sector, industry, "
                   + "description, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindEditableColumns(statement, stock);
            statement.setBoolean(9, stock.isActive());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    stock.setId(keys.getInt(1));
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("A stock with this symbol already exists on that exchange.");
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the stock.", e);
        }
    }

    public boolean update(Stock stock) {
        String sql = "UPDATE stocks SET company_name = ?, symbol = ?, exchange = ?, api_symbol = ?, currency = ?, "
                   + "sector = ?, industry = ?, description = ?, is_active = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindEditableColumns(statement, stock);
            statement.setBoolean(9, stock.isActive());
            statement.setInt(10, stock.getId());
            return statement.executeUpdate() == 1;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("A stock with this symbol already exists on that exchange.");
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the stock.", e);
        }
    }

    /** Market cap and 52-week range, written only with values a provider really returned. */
    public void updateFundamentals(Stock stock) {
        String sql = "UPDATE stocks SET market_cap = ?, week52_high = ?, week52_low = ?, fundamentals_source = ?, "
                   + "fundamentals_updated_at = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            JdbcUtil.setDecimal(statement, 1, stock.getMarketCap());
            JdbcUtil.setDecimal(statement, 2, stock.getWeek52High());
            JdbcUtil.setDecimal(statement, 3, stock.getWeek52Low());
            statement.setString(4, stock.getFundamentalsSource());
            JdbcUtil.setDateTime(statement, 5, stock.getFundamentalsUpdatedAt());
            statement.setInt(6, stock.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the company fundamentals.", e);
        }
    }

    public boolean updateActive(int stockId, boolean active) {
        return execute("UPDATE stocks SET is_active = ? WHERE id = ?", active, stockId) == 1;
    }

    /** Permanently removes a stock with its prices, recommendations and watchlist rows. */
    public boolean delete(int stockId) {
        return execute("DELETE FROM stocks WHERE id = ?", stockId) == 1;
    }

    public int countActive() {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM stocks WHERE is_active = TRUE");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Could not count the stocks.", e);
        }
    }

    // ------------------------------------------------------ recently viewed

    /** Remembers that the user opened this stock (one row per user and stock). */
    public void recordView(int userId, int stockId) {
        execute("INSERT INTO stock_views (user_id, stock_id) VALUES (?, ?) "
              + "ON DUPLICATE KEY UPDATE viewed_at = CURRENT_TIMESTAMP", userId, stockId);
    }

    public List<Stock> findRecentlyViewed(int userId, int limit) {
        return query(SELECT_STOCKS + "JOIN stock_views v ON v.stock_id = s.id "
                   + "WHERE v.user_id = ? AND s.is_active = TRUE ORDER BY v.viewed_at DESC LIMIT ?", userId, limit);
    }

    // -------------------------------------------------------------- helpers

    private void bindEditableColumns(PreparedStatement statement, Stock stock) throws SQLException {
        statement.setString(1, stock.getCompanyName());
        statement.setString(2, stock.getSymbol());
        statement.setString(3, stock.getExchange());
        statement.setString(4, stock.getApiSymbol());
        statement.setString(5, stock.getCurrency());
        statement.setString(6, stock.getSector());
        statement.setString(7, stock.getIndustry());
        statement.setString(8, stock.getDescription());
    }

    private List<Stock> query(String sql, Object... parameters) {
        List<Stock> stocks = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    stocks.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the stocks.", e);
        }
        return stocks;
    }

    private List<String> strings(String sql) {
        List<String> values = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                values.add(resultSet.getString(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the filter values.", e);
        }
        return values;
    }

    private int execute(String sql, Object... parameters) {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the stock.", e);
        }
    }

    private Stock mapRow(ResultSet resultSet) throws SQLException {
        Stock stock = new Stock();
        stock.setId(resultSet.getInt("id"));
        stock.setCompanyName(resultSet.getString("company_name"));
        stock.setSymbol(resultSet.getString("symbol"));
        stock.setExchange(resultSet.getString("exchange"));
        stock.setApiSymbol(resultSet.getString("api_symbol"));
        stock.setCurrency(resultSet.getString("currency"));
        stock.setSector(resultSet.getString("sector"));
        stock.setIndustry(resultSet.getString("industry"));
        stock.setDescription(resultSet.getString("description"));
        stock.setMarketCap(resultSet.getBigDecimal("market_cap"));
        stock.setWeek52High(resultSet.getBigDecimal("week52_high"));
        stock.setWeek52Low(resultSet.getBigDecimal("week52_low"));
        stock.setFundamentalsSource(resultSet.getString("fundamentals_source"));
        stock.setFundamentalsUpdatedAt(JdbcUtil.getDateTime(resultSet, "fundamentals_updated_at"));
        stock.setActive(resultSet.getBoolean("is_active"));
        stock.setCreatedAt(JdbcUtil.getDateTime(resultSet, "created_at"));
        stock.setUpdatedAt(JdbcUtil.getDateTime(resultSet, "updated_at"));
        return stock;
    }
}
