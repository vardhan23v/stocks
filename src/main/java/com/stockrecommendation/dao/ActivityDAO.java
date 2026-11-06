package com.stockrecommendation.dao;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.stockrecommendation.model.ActivityLog;
import com.stockrecommendation.util.JdbcUtil;

/** SQL for the activity_logs table (system activity trail). */
@Repository
public class ActivityDAO {

    private final JdbcTemplate jdbc;

    public ActivityDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Writes one line. A failure here is swallowed on purpose: losing an audit
     * line must never break the action the user actually asked for.
     */
    public void log(Integer userId, String action, String details) {
        try {
            jdbc.update("INSERT INTO activity_logs (user_id, action, details) VALUES (?, ?, ?)",
                    userId, action, details == null ? null : details.substring(0, Math.min(500, details.length())));
        } catch (DataAccessException e) {
            // ignored: see method comment
        }
    }

    public List<ActivityLog> findRecent(int limit) {
        return jdbc.query("SELECT a.id, a.user_id, u.name AS user_name, a.action, a.details, a.created_at "
                + "FROM activity_logs a LEFT JOIN users u ON u.id = a.user_id ORDER BY a.id DESC LIMIT ?", (rs, row) -> {
                    ActivityLog log = new ActivityLog();
                    log.setId(rs.getLong("id"));
                    int userId = rs.getInt("user_id");
                    log.setUserId(rs.wasNull() ? null : userId);
                    log.setUserName(rs.getString("user_name"));
                    log.setAction(rs.getString("action"));
                    log.setDetails(rs.getString("details"));
                    log.setCreatedAt(JdbcUtil.getDateTime(rs, "created_at"));
                    return log;
                }, limit);
    }
}
