package com.stockrecommendation.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Read-only access to app.properties (market data provider settings).
 *
 * Lookup order for every key: JVM system property, environment variable,
 * then the file. The API key additionally accepts ALPHA_VANTAGE_API_KEY, the
 * name Alpha Vantage uses in its own examples.
 */
public final class AppConfig {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (input != null) {
                PROPERTIES.load(input);
            }
        } catch (IOException e) {
            // Every setting has a default, so a missing file simply means demo mode.
        }
    }

    private AppConfig() {
        // utility class: never instantiated
    }

    public static String get(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key.toUpperCase().replace('.', '_'));
        }
        if (value == null && "market.apiKey".equals(key)) {
            value = System.getenv("ALPHA_VANTAGE_API_KEY");
        }
        if (value == null) {
            value = PROPERTIES.getProperty(key, defaultValue);
        }
        return value == null ? defaultValue : value.trim();
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
