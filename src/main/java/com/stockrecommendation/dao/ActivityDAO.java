package com.stockrecommendation.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.stockrecommendation.model.ActivityLog;
import com.stockrecommendation.util.DBConnection;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.JdbcUtil;

/** SQL for the activity_logs table (system activity trail). */
public class ActivityDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Writes one line. A failure here is swallowed on purpose: losing an audit
     * line must never break the action the user actually asked for.
     */
    public void log(Integer userId, String action, String details) {
        String sql = "INSERT INTO activity_logs (user_id, action, details) VALUES (?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (userId == null) {
                statement.setNull(1, Types.INTEGER);
            } else {
                statement.setInt(1, userId);
            }
            statement.setString(2, action);
            statement.setString(3, details == null ? null : details.substring(0, Math.min(500, details.length())));
            statement.executeUpdate();
        } catch (SQLException e) {
            // ignored: see method comment
        }
    }

    public List<ActivityLog> findRecent(int limit) {
        String sql = "SELECT a.id, a.user_id, u.name AS user_name, a.action, a.details, a.created_at "
                   + "FROM activity_logs a LEFT JOIN users u ON u.id = a.user_id ORDER BY a.id DESC LIMIT ?";
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ActivityLog log = new ActivityLog();
                    log.setId(resultSet.getLong("id"));
                    int userId = resultSet.getInt("user_id");
                    log.setUserId(resultSet.wasNull() ? null : userId);
                    log.setUserName(resultSet.getString("user_name"));
                    log.setAction(resultSet.getString("action"));
                    log.setDetails(resultSet.getString("details"));
                    log.setCreatedAt(JdbcUtil.getDateTime(resultSet, "created_at"));
                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the activity log.", e);
        }
        return logs;
    }
}
