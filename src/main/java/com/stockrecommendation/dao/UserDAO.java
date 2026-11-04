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

import com.stockrecommendation.model.User;
import com.stockrecommendation.util.DBConnection;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.JdbcUtil;

/**
 * All SQL that touches the users table. Every statement uses ? placeholders,
 * so user input is never concatenated into SQL.
 */
public class UserDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String SELECT_USERS =
            "SELECT u.id, u.name, u.email, u.password_hash, u.role, u.is_active, u.created_at, u.updated_at, "
          + "(SELECT COUNT(*) FROM watchlists w WHERE w.user_id = u.id) AS watchlist_count "
          + "FROM users u ";

    /** @return the user with this email (case-insensitive), or null */
    public User findByEmail(String email) {
        return findOne(SELECT_USERS + "WHERE u.email = ?", email);
    }

    public User findById(int userId) {
        return findOne(SELECT_USERS + "WHERE u.id = ?", userId);
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_USERS + "ORDER BY u.created_at DESC");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(mapRow(resultSet));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the users.", e);
        }
        return users;
    }

    /** Inserts the user and stores the generated id in it. */
    public void create(User user) {
        String sql = "INSERT INTO users (name, email, password_hash, role, is_active) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getRole());
            statement.setBoolean(5, user.isActive());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("An account with that email already exists.");
        } catch (SQLException e) {
            throw new DataAccessException("Could not create the account.", e);
        }
    }

    public boolean updateName(int userId, String name) {
        return update("UPDATE users SET name = ? WHERE id = ?", name, userId);
    }

    public boolean updatePassword(int userId, String passwordHash) {
        return update("UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, userId);
    }

    public boolean updateActive(int userId, boolean active) {
        return update("UPDATE users SET is_active = ? WHERE id = ?", active, userId);
    }

    public boolean updateRole(int userId, String role) {
        return update("UPDATE users SET role = ? WHERE id = ?", role, userId);
    }

    /** Deletes a user; the watchlist and view rows go with it (ON DELETE CASCADE). */
    public boolean delete(int userId) {
        return update("DELETE FROM users WHERE id = ?", userId);
    }

    public int count() {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Could not count the users.", e);
        }
    }

    private User findOne(String sql, Object parameter) {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the user account.", e);
        }
    }

    private boolean update(String sql, Object... parameters) {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the user account.", e);
        }
    }

    private User mapRow(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getInt("id"));
        user.setName(resultSet.getString("name"));
        user.setEmail(resultSet.getString("email"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setRole(resultSet.getString("role"));
        user.setActive(resultSet.getBoolean("is_active"));
        user.setCreatedAt(JdbcUtil.getDateTime(resultSet, "created_at"));
        user.setUpdatedAt(JdbcUtil.getDateTime(resultSet, "updated_at"));
        user.setWatchlistCount(resultSet.getInt("watchlist_count"));
        return user;
    }
}
