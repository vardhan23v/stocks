package com.stockrecommendation.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.stockrecommendation.service.MarketDataService;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Application start and stop hooks.
 *
 * Start: when the setting db.init is true (environment variable DB_INIT=true)
 * and the database has no users table yet, schema.sql and sample-data.sql
 * from the WAR are executed once. This is for hosts such as Railway where the
 * database is only reachable from inside the platform. It never runs on a
 * database that already has tables, and it is off by default.
 *
 * Stop: the market data background thread is shut down.
 */
@WebListener
public class AppLifecycleListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        if (!"true".equalsIgnoreCase(AppConfig.get("db.init", "false"))) {
            return;
        }
        ServletContext context = event.getServletContext();
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables "
                  + "WHERE table_schema = 'stock_recommendation_db' AND table_name = 'users'")) {
                resultSet.next();
                if (resultSet.getInt(1) > 0) {
                    context.log("DB_INIT: tables already exist, nothing to do.");
                    return;
                }
            }
            int schema = runScript(statement, "schema.sql");
            int sample = runScript(statement, "sample-data.sql");
            context.log("DB_INIT: database created (" + schema + " schema statements, "
                    + sample + " sample data statements).");
        } catch (SQLException | IOException e) {
            context.log("DB_INIT failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        MarketDataService.shutdown();
    }

    /**
     * Runs a script from the classpath. Statements end with ';' at the end of
     * a line, which is how schema.sql and sample-data.sql are written; full-line
     * "--" comments are skipped. All statements share one connection, so
     * session variables such as @stock_id keep working.
     */
    private static int runScript(Statement statement, String resource) throws IOException, SQLException {
        InputStream input = AppLifecycleListener.class.getClassLoader().getResourceAsStream(resource);
        if (input == null) {
            throw new IOException(resource + " is not in the WAR.");
        }
        int count = 0;
        StringBuilder sql = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (sql.length() == 0 && (trimmed.isEmpty() || trimmed.startsWith("--"))) {
                    continue;
                }
                sql.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    statement.execute(sql.substring(0, sql.lastIndexOf(";")));
                    sql.setLength(0);
                    count++;
                }
            }
        }
        return count;
    }
}
