package com.stockrecommendation.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.stockrecommendation.model.User;
import com.stockrecommendation.util.JdbcUtil;

/** All SQL that touches the users table. Parameters are always bound with '?'. */
@Repository
public class UserDAO {

    private static final String SELECT_USERS =
            "SELECT u.id, u.name, u.email, u.password_hash, u.role, u.is_active, u.created_at, u.updated_at, "
          + "(SELECT COUNT(*) FROM watchlists w WHERE w.user_id = u.id) AS watchlist_count "
          + "FROM users u ";

    private static final RowMapper<User> MAPPER = (rs, row) -> {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(rs.getString("role"));
        user.setActive(rs.getBoolean("is_active"));
        user.setCreatedAt(JdbcUtil.getDateTime(rs, "created_at"));
        user.setUpdatedAt(JdbcUtil.getDateTime(rs, "updated_at"));
        user.setWatchlistCount(rs.getInt("watchlist_count"));
        return user;
    };

    private final JdbcTemplate jdbc;

    public UserDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @return the user with this email, or null */
    public User findByEmail(String email) {
        return jdbc.query(SELECT_USERS + "WHERE u.email = ?", MAPPER, email).stream().findFirst().orElse(null);
    }

    public User findById(int userId) {
        return jdbc.query(SELECT_USERS + "WHERE u.id = ?", MAPPER, userId).stream().findFirst().orElse(null);
    }

    public List<User> findAll() {
        return jdbc.query(SELECT_USERS + "ORDER BY u.created_at DESC", MAPPER);
    }

    /** Inserts the user and stores the generated id in it. */
    public void create(User user) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO users (name, email, password_hash, role, is_active) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, user.getName());
                statement.setString(2, user.getEmail());
                statement.setString(3, user.getPasswordHash());
                statement.setString(4, user.getRole());
                statement.setBoolean(5, user.isActive());
                return statement;
            }, keys);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("An account with that email already exists.");
        }
        user.setId(keys.getKey().intValue());
    }

    public boolean updateName(int userId, String name) {
        return jdbc.update("UPDATE users SET name = ? WHERE id = ?", name, userId) == 1;
    }

    public boolean updatePassword(int userId, String passwordHash) {
        return jdbc.update("UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, userId) == 1;
    }

    public boolean updateActive(int userId, boolean active) {
        return jdbc.update("UPDATE users SET is_active = ? WHERE id = ?", active, userId) == 1;
    }

    public boolean updateRole(int userId, String role) {
        return jdbc.update("UPDATE users SET role = ? WHERE id = ?", role, userId) == 1;
    }

    /** Deletes a user; watchlist and view rows go with it (ON DELETE CASCADE). */
    public boolean delete(int userId) {
        return jdbc.update("DELETE FROM users WHERE id = ?", userId) == 1;
    }

    public int count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
    }
}
