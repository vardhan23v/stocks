package com.stockrecommendation.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

import com.stockrecommendation.model.Stock;
import com.stockrecommendation.model.Watchlist;
import com.stockrecommendation.util.DBConnection;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.JdbcUtil;

/**
 * SQL for the watchlists table.
 *
 * Every read and delete has "user_id = ?" in its WHERE clause, and the id
 * always comes from the logged-in session user (never from the request), so
 * one user can never read or change another user's watchlist.
 */
public class WatchlistDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    public List<Watchlist> findByUser(int userId) {
        String sql = "SELECT w.id, w.user_id, w.stock_id, w.added_at, s.company_name, s.symbol, s.exchange, s.currency, "
                   + "s.sector, s.is_active FROM watchlists w JOIN stocks s ON s.id = w.stock_id "
                   + "WHERE w.user_id = ? ORDER BY w.added_at DESC";
        List<Watchlist> entries = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Watchlist entry = new Watchlist();
                    entry.setId(resultSet.getInt("id"));
                    entry.setUserId(resultSet.getInt("user_id"));
                    entry.setStockId(resultSet.getInt("stock_id"));
                    entry.setAddedAt(JdbcUtil.getDateTime(resultSet, "added_at"));

                    Stock stock = new Stock();
                    stock.setId(entry.getStockId());
                    stock.setCompanyName(resultSet.getString("company_name"));
                    stock.setSymbol(resultSet.getString("symbol"));
                    stock.setExchange(resultSet.getString("exchange"));
                    stock.setCurrency(resultSet.getString("currency"));
                    stock.setSector(resultSet.getString("sector"));
                    stock.setActive(resultSet.getBoolean("is_active"));
                    entry.setStock(stock);
                    entries.add(entry);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load your watchlist.", e);
        }
        return entries;
    }

    public boolean exists(int userId, int stockId) {
        String sql = "SELECT 1 FROM watchlists WHERE user_id = ? AND stock_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, stockId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check your watchlist.", e);
        }
    }

    /**
     * @return false when the UNIQUE (user_id, stock_id) constraint rejected a
     *         duplicate - the final guard against two simultaneous clicks
     */
    public boolean insert(int userId, int stockId) {
        String sql = "INSERT INTO watchlists (user_id, stock_id) VALUES (?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, stockId);
            return statement.executeUpdate() == 1;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false;
        } catch (SQLException e) {
            throw new DataAccessException("Could not add the stock to your watchlist.", e);
        }
    }

    public boolean delete(int userId, int stockId) {
        String sql = "DELETE FROM watchlists WHERE user_id = ? AND stock_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, stockId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not remove the stock from your watchlist.", e);
        }
    }

    public int countAll() {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM watchlists");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Could not count the watchlist entries.", e);
        }
    }
}
