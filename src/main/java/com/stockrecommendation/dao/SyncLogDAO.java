package com.stockrecommendation.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.stockrecommendation.model.ApiSyncLog;
import com.stockrecommendation.util.DBConnection;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.JdbcUtil;

/** SQL for the api_sync_logs table. */
public class SyncLogDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String SELECT_LOGS =
            "SELECT id, provider, symbol, status, response_message, synced_at FROM api_sync_logs ";

    public void insert(String provider, String symbol, String status, String message) {
        String sql = "INSERT INTO api_sync_logs (provider, symbol, status, response_message) VALUES (?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, provider);
            statement.setString(2, symbol);
            statement.setString(3, status);
            statement.setString(4, message == null ? null : message.substring(0, Math.min(500, message.length())));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not write the sync log.", e);
        }
    }

    public List<ApiSyncLog> findRecent(int limit) {
        return query(SELECT_LOGS + "ORDER BY id DESC LIMIT ?", limit);
    }

    /** The newest SUCCESS row of any provider, or null. */
    public ApiSyncLog findLatestSuccess() {
        List<ApiSyncLog> logs = query(SELECT_LOGS + "WHERE status = 'SUCCESS' ORDER BY id DESC LIMIT 1");
        return logs.isEmpty() ? null : logs.get(0);
    }

    /** The newest row of any status, or null. */
    public ApiSyncLog findLatest() {
        List<ApiSyncLog> logs = query(SELECT_LOGS + "ORDER BY id DESC LIMIT 1");
        return logs.isEmpty() ? null : logs.get(0);
    }

    /** Real API calls (not cache hits, not demo) made today - for the free-tier daily quota. */
    public int countCallsToday(String provider) {
        String sql = "SELECT COUNT(*) FROM api_sync_logs WHERE provider = ? AND status IN "
                   + "('SUCCESS', 'RATE_LIMITED', 'ERROR', 'NO_DATA') AND synced_at >= CURDATE()";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, provider);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not count today's API calls.", e);
        }
    }

    private List<ApiSyncLog> query(String sql, Object... parameters) {
        List<ApiSyncLog> logs = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ApiSyncLog log = new ApiSyncLog();
                    log.setId(resultSet.getLong("id"));
                    log.setProvider(resultSet.getString("provider"));
                    log.setSymbol(resultSet.getString("symbol"));
                    log.setStatus(resultSet.getString("status"));
                    log.setResponseMessage(resultSet.getString("response_message"));
                    log.setSyncedAt(JdbcUtil.getDateTime(resultSet, "synced_at"));
                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the sync log.", e);
        }
        return logs;
    }
}
