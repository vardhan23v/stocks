package com.stockrecommendation.api;

/**
 * A market data request that did not produce usable data. The status is one
 * of the ApiSyncLog constants (RATE_LIMITED, NO_DATA, ERROR) and is written
 * to api_sync_logs together with the message.
 */
public class MarketDataException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String status;

    public MarketDataException(String status, String message) {
        super(message);
        this.status = status;
    }

    public MarketDataException(String status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
