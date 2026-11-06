package com.stockrecommendation.dao;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.stockrecommendation.model.Stock;
import com.stockrecommendation.model.Watchlist;
import com.stockrecommendation.util.JdbcUtil;

/**
 * SQL for the watchlists table.
 *
 * Every read and delete has "user_id = ?" in its WHERE clause, and the id
 * always comes from the logged-in user (never from the request), so one user
 * can never read or change another user's watchlist.
 */
@Repository
public class WatchlistDAO {

    private final JdbcTemplate jdbc;

    public WatchlistDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Watchlist> findByUser(int userId) {
        return jdbc.query("SELECT w.id, w.user_id, w.stock_id, w.added_at, s.company_name, s.symbol, s.exchange, s.currency, "
                + "s.sector, s.is_active FROM watchlists w JOIN stocks s ON s.id = w.stock_id "
                + "WHERE w.user_id = ? ORDER BY w.added_at DESC", (rs, row) -> {
                    Watchlist entry = new Watchlist();
                    entry.setId(rs.getInt("id"));
                    entry.setUserId(rs.getInt("user_id"));
                    entry.setStockId(rs.getInt("stock_id"));
                    entry.setAddedAt(JdbcUtil.getDateTime(rs, "added_at"));
                    Stock stock = new Stock();
                    stock.setId(entry.getStockId());
                    stock.setCompanyName(rs.getString("company_name"));
                    stock.setSymbol(rs.getString("symbol"));
                    stock.setExchange(rs.getString("exchange"));
                    stock.setCurrency(rs.getString("currency"));
                    stock.setSector(rs.getString("sector"));
                    stock.setActive(rs.getBoolean("is_active"));
                    entry.setStock(stock);
                    return entry;
                }, userId);
    }

    public boolean exists(int userId, int stockId) {
        return !jdbc.queryForList("SELECT 1 FROM watchlists WHERE user_id = ? AND stock_id = ?", Integer.class, userId, stockId).isEmpty();
    }

    /** @return false when UNIQUE (user_id, stock_id) rejected a duplicate */
    public boolean insert(int userId, int stockId) {
        try {
            return jdbc.update("INSERT INTO watchlists (user_id, stock_id) VALUES (?, ?)", userId, stockId) == 1;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    public boolean delete(int userId, int stockId) {
        return jdbc.update("DELETE FROM watchlists WHERE user_id = ? AND stock_id = ?", userId, stockId) == 1;
    }

    public int countAll() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM watchlists", Integer.class);
    }
}
