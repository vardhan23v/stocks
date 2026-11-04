package com.stockrecommendation.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Creates JDBC connections for the whole application.
 *
 * The settings are read once from db.properties on the classpath
 * (src/main/resources/db.properties). A JVM system property with the same
 * name (-Ddb.password=...) or an environment variable (DB_PASSWORD) wins over
 * the file, so no credentials are hard coded anywhere in the Java code.
 *
 * Usage in a DAO:
 *
 *   try (Connection connection = DBConnection.getConnection();
 *        PreparedStatement statement = connection.prepareStatement(SQL)) {
 *       ...
 *   }
 */
public final class DBConnection {

    private static final String PROPERTIES_FILE = "db.properties";

    private static final Properties PROPERTIES = new Properties();

    /** Non-null when the configuration could not be loaded. */
    private static String configError;

    static {
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                configError = "db.properties was not found on the classpath. It belongs in src/main/resources.";
            } else {
                PROPERTIES.load(input);
                // Optional with JDBC 4, but writing it down makes the driver
                // requirement visible and gives a clear error when it is missing.
                Class.forName(PROPERTIES.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
            }
        } catch (IOException e) {
            configError = "db.properties could not be read: " + e.getMessage();
        } catch (ClassNotFoundException e) {
            configError = "The MySQL JDBC driver is not on the classpath. Check the pom.xml and redeploy the WAR.";
        }
    }

    private DBConnection() {
        // utility class: never instantiated
    }

    /**
     * @return a new connection to the stock_recommendation_db database
     * @throws SQLException when MySQL is unreachable or the credentials are wrong
     */
    public static Connection getConnection() throws SQLException {
        if (configError != null) {
            throw new SQLException(configError);
        }
        return DriverManager.getConnection(setting("db.url"), setting("db.user"), setting("db.password"));
    }

    /** The JDBC URL currently configured - used by the connection test. */
    public static String getUrl() {
        String url = setting("db.url");
        return url.isEmpty() ? "(not configured)" : url;
    }

    /** System property, then environment variable (db.user -> DB_USER), then db.properties. */
    private static String setting(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key.toUpperCase().replace('.', '_'));
        }
        if (value == null) {
            value = PROPERTIES.getProperty(key, "");
        }
        return value;
    }
}
