package com.stockrecommendation.api;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockrecommendation.model.ApiSyncLog;
import com.stockrecommendation.model.StockPrice;

/**
 * HTTP client for the Alpha Vantage REST API (https://www.alphavantage.co/documentation/),
 * built on java.net.http.HttpClient, with Jackson for the JSON.
 *
 * Endpoints used:
 *   TIME_SERIES_DAILY  daily OHLCV, outputsize=compact (latest 100 bars - the free tier limit)
 *   OVERVIEW           market capitalisation and 52-week range (US listings only)
 *
 * Alpha Vantage answers HTTP 200 even for errors and signals them in the body:
 *   "Error Message"          unknown symbol or bad parameter        -> NO_DATA / ERROR
 *   "Note" / "Information"   rate limit reached or premium endpoint -> RATE_LIMITED
 * Both are turned into a MarketDataException with that status.
 *
 * The API key is passed in by MarketDataService from configuration and is
 * never written to logs or error messages.
 */
public class MarketDataClient {

    public static final String PROVIDER = "ALPHA_VANTAGE";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final Duration timeout;

    public MarketDataClient(String baseUrl, String apiKey, int timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    /** Daily bars for one symbol, oldest first. */
    public MarketDataResponse fetchDaily(String symbol) throws MarketDataException {
        JsonNode body = get(Map.of("function", "TIME_SERIES_DAILY", "symbol", symbol, "outputsize", "compact"));
        JsonNode series = body.get("Time Series (Daily)");
        if (series == null || !series.isObject() || series.isEmpty()) {
            throw new MarketDataException(ApiSyncLog.NO_DATA, "The provider returned no daily prices for " + symbol + ".");
        }

        MarketDataResponse response = new MarketDataResponse(PROVIDER, symbol);
        // TreeMap sorts the "yyyy-MM-dd" keys, which gives oldest-first order.
        TreeMap<String, JsonNode> sorted = new TreeMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = series.fields(); it.hasNext();) {
            Map.Entry<String, JsonNode> entry = it.next();
            sorted.put(entry.getKey(), entry.getValue());
        }
        try {
            for (Map.Entry<String, JsonNode> entry : sorted.entrySet()) {
                JsonNode bar = entry.getValue();
                StockPrice price = new StockPrice(
                        LocalDate.parse(entry.getKey()).atStartOfDay(),
                        decimal(bar, "1. open"), decimal(bar, "2. high"), decimal(bar, "3. low"),
                        decimal(bar, "4. close"),
                        bar.has("5. volume") ? Long.parseLong(bar.get("5. volume").asText()) : null,
                        PROVIDER);
                response.getBars().add(price);
            }
        } catch (RuntimeException e) {
            throw new MarketDataException(ApiSyncLog.ERROR, "The provider response for " + symbol + " could not be parsed.", e);
        }
        response.setLastRefreshed(LocalDate.parse(sorted.lastKey()).atStartOfDay());
        return response;
    }

    /**
     * Market capitalisation and 52-week range. Missing fields stay null - a
     * value is never filled in when the provider did not supply it.
     */
    public MarketDataResponse fetchOverview(String symbol) throws MarketDataException {
        JsonNode body = get(Map.of("function", "OVERVIEW", "symbol", symbol));
        MarketDataResponse response = new MarketDataResponse(PROVIDER, symbol);
        response.setMarketCap(optionalDecimal(body, "MarketCapitalization"));
        response.setWeek52High(optionalDecimal(body, "52WeekHigh"));
        response.setWeek52Low(optionalDecimal(body, "52WeekLow"));
        response.setLastRefreshed(LocalDateTime.now());
        return response;
    }

    private JsonNode get(Map<String, String> parameters) throws MarketDataException {
        StringBuilder url = new StringBuilder(baseUrl).append('?');
        parameters.forEach((key, value) -> url.append(encode(key)).append('=').append(encode(value)).append('&'));
        url.append("apikey=").append(encode(apiKey));

        HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString()))
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new MarketDataException(ApiSyncLog.ERROR, "Could not reach the market data provider: " + e.getClass().getSimpleName(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MarketDataException(ApiSyncLog.ERROR, "The market data request was interrupted.", e);
        }

        if (response.statusCode() == 429) {
            throw new MarketDataException(ApiSyncLog.RATE_LIMITED, "The provider rejected the request: too many requests (HTTP 429).");
        }
        if (response.statusCode() != 200) {
            throw new MarketDataException(ApiSyncLog.ERROR, "The provider answered with HTTP " + response.statusCode() + ".");
        }

        JsonNode body;
        try {
            body = JSON.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new MarketDataException(ApiSyncLog.ERROR, "The provider did not return valid JSON.", e);
        }
        if (body == null || !body.isObject()) {
            throw new MarketDataException(ApiSyncLog.ERROR, "The provider did not return a JSON object.");
        }

        if (body.has("Note") || body.has("Information")) {
            String text = body.has("Note") ? body.get("Note").asText() : body.get("Information").asText();
            throw new MarketDataException(ApiSyncLog.RATE_LIMITED, "Provider limit or plan restriction: " + scrub(text));
        }
        if (body.has("Error Message")) {
            String text = body.get("Error Message").asText();
            String status = text.toLowerCase().contains("apikey") ? ApiSyncLog.ERROR : ApiSyncLog.NO_DATA;
            throw new MarketDataException(status, "Provider error: " + scrub(text));
        }
        return body;
    }

    /** Removes the API key from any text that came back from the provider. */
    private String scrub(String text) {
        return apiKey == null || apiKey.isEmpty() ? text : text.replace(apiKey, "***");
    }

    private static BigDecimal decimal(JsonNode bar, String field) {
        return new BigDecimal(bar.get(field).asText());
    }

    private static BigDecimal optionalDecimal(JsonNode body, String field) {
        if (!body.has(field) || body.get(field).isNull()) {
            return null;
        }
        try {
            return new BigDecimal(body.get(field).asText());
        } catch (NumberFormatException e) {
            return null;   // Alpha Vantage writes "None" for unknown values
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
