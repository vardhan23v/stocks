package com.stockrecommendation.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** One attempt to fetch market data, successful or not. */
public class ApiSyncLog implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SUCCESS = "SUCCESS";
    public static final String CACHED = "CACHED";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String ERROR = "ERROR";
    public static final String NO_DATA = "NO_DATA";
    public static final String DEMO = "DEMO";

    private long id;
    private String provider;
    private String symbol;
    private String status;
    private String responseMessage;
    private LocalDateTime syncedAt;

    public ApiSyncLog() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }

    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }
}
