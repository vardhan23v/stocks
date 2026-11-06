package com.stockrecommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The market.* settings from application.properties (each one can be
 * overridden by an environment variable, see that file).
 */
@ConfigurationProperties(prefix = "market")
public class MarketProperties {

    /** DEMO or ALPHA_VANTAGE. */
    private String provider = "DEMO";
    private String apiKey = "";
    private String baseUrl = "https://www.alphavantage.co/query";
    private int cacheMinutes = 720;
    private int minIntervalMs = 13000;
    private int timeoutSeconds = 15;
    private int dailyQuota = 25;

    public String getProvider() { return provider == null ? "DEMO" : provider.trim().toUpperCase(); }
    public void setProvider(String provider) { this.provider = provider; }

    public String getApiKey() { return apiKey == null ? "" : apiKey.trim(); }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public int getCacheMinutes() { return cacheMinutes; }
    public void setCacheMinutes(int cacheMinutes) { this.cacheMinutes = cacheMinutes; }

    public int getMinIntervalMs() { return minIntervalMs; }
    public void setMinIntervalMs(int minIntervalMs) { this.minIntervalMs = minIntervalMs; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public int getDailyQuota() { return dailyQuota; }
    public void setDailyQuota(int dailyQuota) { this.dailyQuota = dailyQuota; }
}
