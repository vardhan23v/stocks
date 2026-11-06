package com.stockrecommendation.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.stockrecommendation.model.ApiSyncLog;
import com.stockrecommendation.util.JdbcUtil;

/** SQL for the api_sync_logs table. */
@Repository
public class SyncLogDAO {

    private static final String SELECT_LOGS =
            "SELECT id, provider, symbol, status, response_message, synced_at FROM api_sync_logs ";

    private static final RowMapper<ApiSyncLog> MAPPER = (rs, row) -> {
        ApiSyncLog log = new ApiSyncLog();
        log.setId(rs.getLong("id"));
        log.setProvider(rs.getString("provider"));
        log.setSymbol(rs.getString("symbol"));
        log.setStatus(rs.getString("status"));
        log.setResponseMessage(rs.getString("response_message"));
        log.setSyncedAt(JdbcUtil.getDateTime(rs, "synced_at"));
        return log;
    };

    private final JdbcTemplate jdbc;

    public SyncLogDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(String provider, String symbol, String status, String message) {
        jdbc.update("INSERT INTO api_sync_logs (provider, symbol, status, response_message) VALUES (?, ?, ?, ?)",
                provider, symbol, status, message == null ? null : message.substring(0, Math.min(500, message.length())));
    }

    public List<ApiSyncLog> findRecent(int limit) {
        return jdbc.query(SELECT_LOGS + "ORDER BY id DESC LIMIT ?", MAPPER, limit);
    }

    public ApiSyncLog findLatestSuccess() {
        return jdbc.query(SELECT_LOGS + "WHERE status = 'SUCCESS' ORDER BY id DESC LIMIT 1", MAPPER).stream().findFirst().orElse(null);
    }

    public ApiSyncLog findLatest() {
        return jdbc.query(SELECT_LOGS + "ORDER BY id DESC LIMIT 1", MAPPER).stream().findFirst().orElse(null);
    }

    /** Real API calls (not cache hits, not demo) made today - for the free-tier daily quota. */
    public int countCallsToday(String provider) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM api_sync_logs WHERE provider = ? AND status IN "
                + "('SUCCESS', 'RATE_LIMITED', 'ERROR', 'NO_DATA') AND synced_at >= CURDATE()", Integer.class, provider);
    }
}
