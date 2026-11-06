package com.stockrecommendation.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.stockrecommendation.model.Stock;
import com.stockrecommendation.util.JdbcUtil;

/**
 * SQL for the stocks table and the small stock_views table ("recently viewed").
 * Quotes are not loaded here - StockService attaches them from StockPriceDAO.
 */
@Repository
public class StockDAO {

    private static final String SELECT_STOCKS =
            "SELECT s.id, s.company_name, s.symbol, s.exchange, s.api_symbol, s.currency, s.sector, s.industry, "
          + "s.description, s.market_cap, s.week52_high, s.week52_low, s.fundamentals_source, "
          + "s.fundamentals_updated_at, s.is_active, s.created_at, s.updated_at FROM stocks s ";

    private static final RowMapper<Stock> MAPPER = (rs, row) -> {
        Stock stock = new Stock();
        stock.setId(rs.getInt("id"));
        stock.setCompanyName(rs.getString("company_name"));
        stock.setSymbol(rs.getString("symbol"));
        stock.setExchange(rs.getString("exchange"));
        stock.setApiSymbol(rs.getString("api_symbol"));
        stock.setCurrency(rs.getString("currency"));
        stock.setSector(rs.getString("sector"));
        stock.setIndustry(rs.getString("industry"));
        stock.setDescription(rs.getString("description"));
        stock.setMarketCap(rs.getBigDecimal("market_cap"));
        stock.setWeek52High(rs.getBigDecimal("week52_high"));
        stock.setWeek52Low(rs.getBigDecimal("week52_low"));
        stock.setFundamentalsSource(rs.getString("fundamentals_source"));
        stock.setFundamentalsUpdatedAt(JdbcUtil.getDateTime(rs, "fundamentals_updated_at"));
        stock.setActive(rs.getBoolean("is_active"));
        stock.setCreatedAt(JdbcUtil.getDateTime(rs, "created_at"));
        stock.setUpdatedAt(JdbcUtil.getDateTime(rs, "updated_at"));
        return stock;
    };

    private final JdbcTemplate jdbc;

    public StockDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Searches by company name or ticker, optionally limited to one sector and/or exchange. */
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
        return jdbc.query(sql.toString(), MAPPER, parameters.toArray());
    }

    public List<Stock> findAllActive() {
        return jdbc.query(SELECT_STOCKS + "WHERE s.is_active = TRUE ORDER BY s.symbol", MAPPER);
    }

    public Stock findById(int stockId) {
        return jdbc.query(SELECT_STOCKS + "WHERE s.id = ?", MAPPER, stockId).stream().findFirst().orElse(null);
    }

    public List<String> findSectors() {
        return jdbc.queryForList("SELECT DISTINCT sector FROM stocks WHERE sector IS NOT NULL AND is_active = TRUE ORDER BY sector", String.class);
    }

    public List<String> findExchanges() {
        return jdbc.queryForList("SELECT DISTINCT exchange FROM stocks WHERE is_active = TRUE ORDER BY exchange", String.class);
    }

    /** Inserts a stock and stores its generated id. */
    public void insert(Stock stock) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO stocks (company_name, symbol, exchange, api_symbol, currency, sector, industry, "
                      + "description, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                Object[] values = editableColumns(stock);
                for (int i = 0; i < values.length; i++) {
                    statement.setObject(i + 1, values[i]);
                }
                statement.setBoolean(9, stock.isActive());
                return statement;
            }, keys);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("A stock with this symbol already exists on that exchange.");
        }
        stock.setId(keys.getKey().intValue());
    }

    public boolean update(Stock stock) {
        Object[] editable = editableColumns(stock);
        Object[] parameters = new Object[editable.length + 2];
        System.arraycopy(editable, 0, parameters, 0, editable.length);
        parameters[editable.length] = stock.isActive();
        parameters[editable.length + 1] = stock.getId();
        try {
            return jdbc.update("UPDATE stocks SET company_name = ?, symbol = ?, exchange = ?, api_symbol = ?, currency = ?, "
                    + "sector = ?, industry = ?, description = ?, is_active = ? WHERE id = ?", parameters) == 1;
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("A stock with this symbol already exists on that exchange.");
        }
    }

    /** Market cap and 52-week range, written only with values a provider really returned. */
    public void updateFundamentals(Stock stock) {
        jdbc.update("UPDATE stocks SET market_cap = ?, week52_high = ?, week52_low = ?, fundamentals_source = ?, "
                + "fundamentals_updated_at = ? WHERE id = ?",
                stock.getMarketCap(), stock.getWeek52High(), stock.getWeek52Low(), stock.getFundamentalsSource(),
                JdbcUtil.timestamp(stock.getFundamentalsUpdatedAt()), stock.getId());
    }

    public boolean updateActive(int stockId, boolean active) {
        return jdbc.update("UPDATE stocks SET is_active = ? WHERE id = ?", active, stockId) == 1;
    }

    /** Permanently removes a stock with its prices, recommendations and watchlist rows. */
    public boolean delete(int stockId) {
        return jdbc.update("DELETE FROM stocks WHERE id = ?", stockId) == 1;
    }

    public int countActive() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM stocks WHERE is_active = TRUE", Integer.class);
    }

    /** Remembers that the user opened this stock (one row per user and stock). */
    public void recordView(int userId, int stockId) {
        jdbc.update("INSERT INTO stock_views (user_id, stock_id) VALUES (?, ?) "
                  + "ON DUPLICATE KEY UPDATE viewed_at = CURRENT_TIMESTAMP", userId, stockId);
    }

    public List<Stock> findRecentlyViewed(int userId, int limit) {
        return jdbc.query(SELECT_STOCKS + "JOIN stock_views v ON v.stock_id = s.id "
                + "WHERE v.user_id = ? AND s.is_active = TRUE ORDER BY v.viewed_at DESC LIMIT ?", MAPPER, userId, limit);
    }

    private static Object[] editableColumns(Stock stock) {
        return new Object[] {stock.getCompanyName(), stock.getSymbol(), stock.getExchange(), stock.getApiSymbol(),
                             stock.getCurrency(), stock.getSector(), stock.getIndustry(), stock.getDescription()};
    }
}
