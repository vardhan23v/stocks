package com.stockrecommendation.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Stand-alone check of the database configuration.
 *
 * Run it in Eclipse with:  right click the file -> Run As -> Java Application
 * or from the terminal with:
 *   mvn compile exec:java -Dexec.mainClass=com.stockrecommendation.util.TestConnection
 *
 * It prints either the MySQL version and the row counts or a readable
 * explanation of what is wrong.
 */
public final class TestConnection {

    private TestConnection() {
        // only the main method is used
    }

    public static void main(String[] args) {
        System.out.println("StockSense - database connection test");
        System.out.println("URL: " + DBConnection.getUrl());

        try (Connection connection = DBConnection.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            System.out.println("Connected successfully.");
            System.out.println("Database : " + metaData.getDatabaseProductName()
                    + " " + metaData.getDatabaseProductVersion());
            System.out.println("Driver   : " + metaData.getDriverName()
                    + " " + metaData.getDriverVersion());
            for (String table : new String[] {"users", "stocks", "stock_prices", "recommendations", "watchlists"}) {
                try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
                     ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    System.out.printf("%-16s %d rows%n", table, resultSet.getLong(1));
                }
            }
        } catch (SQLException e) {
            System.out.println("Connection FAILED: " + e.getMessage());
            System.out.println("Check that MySQL is running, that schema.sql was loaded and that db.properties matches your credentials.");
        }
    }
}
