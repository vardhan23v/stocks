package com.stockrecommendation.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/** Null-safe conversions from JDBC types used by the RowMappers. */
public final class JdbcUtil {

    private JdbcUtil() {
        // utility class: never instantiated
    }

    public static LocalDateTime getDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    public static Long getLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    /** null-safe LocalDateTime -> Timestamp for JdbcTemplate parameters. */
    public static Timestamp timestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    /** Escapes % and _ so a search term is matched literally inside LIKE '%...%'. */
    public static String likePattern(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
