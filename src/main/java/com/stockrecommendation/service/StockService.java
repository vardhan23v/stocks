package com.stockrecommendation.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.stockrecommendation.dao.ActivityDAO;
import com.stockrecommendation.dao.StockDAO;
import com.stockrecommendation.dao.StockPriceDAO;
import com.stockrecommendation.model.Stock;
import com.stockrecommendation.model.StockPrice;
import com.stockrecommendation.model.User;

/**
 * Stock listing, search and administration.
 *
 * Every stock returned for display carries its latest stored quote from the
 * preferred data source (see MarketDataService.preferredSources), with the
 * source and timestamp - never a number without provenance.
 */
public class StockService implements Serializable {

    private static final long serialVersionUID = 1L;


    public static final String SORT_SYMBOL = "symbol";
    public static final String SORT_NAME = "name";
    public static final String SORT_PRICE = "price";
    public static final String SORT_CHANGE_DESC = "change_desc";
    public static final String SORT_CHANGE_ASC = "change_asc";

    private static final Pattern SYMBOL = Pattern.compile("^[A-Z0-9.&-]{1,20}$");
    private static final Pattern API_SYMBOL = Pattern.compile("^[A-Z0-9.&:-]{1,40}$");
    private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");

    private final StockDAO stockDAO = new StockDAO();
    private final StockPriceDAO priceDAO = new StockPriceDAO();
    private final ActivityDAO activityDAO = new ActivityDAO();

    // ------------------------------------------------------------ reading

    public List<Stock> search(String text, String sector, String exchange, String sort) {
        List<Stock> stocks = stockDAO.search(text, sector, exchange, false);
        attachQuotes(stocks);
        sortStocks(stocks, sort);
        return stocks;
    }

    /** All stocks including inactive ones - for the admin page. */
    public List<Stock> findAllForAdmin(String text) {
        List<Stock> stocks = stockDAO.search(text, null, null, true);
        attachQuotes(stocks);
        return stocks;
    }

    public List<Stock> findAllActiveWithQuotes() {
        List<Stock> stocks = stockDAO.findAllActive();
        attachQuotes(stocks);
        return stocks;
    }

    /** Biggest risers (ascending=false) or fallers (ascending=true) between the last two stored closes. */
    public List<Stock> topMovers(List<Stock> stocks, boolean fallers, int limit) {
        return stocks.stream()
                .filter(stock -> stock.getPercentChange() != null)
                .filter(stock -> fallers ? stock.getPercentChange().signum() < 0 : stock.getPercentChange().signum() > 0)
                .sorted(fallers
                        ? Comparator.comparing(Stock::getPercentChange)
                        : Comparator.comparing(Stock::getPercentChange).reversed())
                .limit(limit)
                .toList();
    }

    public Stock getStock(int stockId) {
        Stock stock = stockDAO.findById(stockId);
        if (stock != null) {
            attachQuotes(List.of(stock));
        }
        return stock;
    }

    public List<String> getSectors() {
        return stockDAO.findSectors();
    }

    public List<String> getExchanges() {
        return stockDAO.findExchanges();
    }

    public int countActive() {
        return stockDAO.countActive();
    }

    public void recordView(User user, int stockId) {
        if (user != null) {
            stockDAO.recordView(user.getId(), stockId);
        }
    }

    public List<Stock> getRecentlyViewed(User user, int limit) {
        List<Stock> stocks = stockDAO.findRecentlyViewed(user.getId(), limit);
        attachQuotes(stocks);
        return stocks;
    }

    /**
     * Copies the newest two bars of the preferred source into each stock:
     * latest close, previous close (for the change), volume, timestamp, source.
     */
    public void attachQuotes(List<Stock> stocks) {
        if (stocks.isEmpty()) {
            return;
        }
        Map<Integer, Map<String, List<StockPrice>>> latest = priceDAO.findLatestTwoBars();
        for (Stock stock : stocks) {
            Map<String, List<StockPrice>> bySource = latest.get(stock.getId());
            if (bySource == null) {
                continue;
            }
            for (String source : MarketDataService.preferredSources()) {
                List<StockPrice> bars = bySource.get(source);
                if (bars != null && !bars.isEmpty()) {
                    StockPrice newest = bars.get(0);
                    stock.setLatestPrice(newest.getClosePrice());
                    stock.setLatestVolume(newest.getVolume());
                    stock.setLastUpdated(newest.getRecordedAt());
                    stock.setDataSource(source);
                    stock.setPreviousClose(bars.size() > 1 ? bars.get(1).getClosePrice() : null);
                    break;
                }
            }
        }
    }

    public static void sortStocks(List<Stock> stocks, String sort) {
        Comparator<BigDecimal> nullsLast = Comparator.nullsLast(Comparator.naturalOrder());
        Comparator<Stock> comparator = switch (sort == null ? SORT_SYMBOL : sort) {
            case SORT_NAME -> Comparator.comparing(Stock::getCompanyName, String.CASE_INSENSITIVE_ORDER);
            case SORT_PRICE -> Comparator.comparing(Stock::getLatestPrice, Comparator.nullsLast(Comparator.reverseOrder()));
            case SORT_CHANGE_DESC -> Comparator.comparing(Stock::getPercentChange, Comparator.nullsLast(Comparator.reverseOrder()));
            case SORT_CHANGE_ASC -> Comparator.comparing(Stock::getPercentChange, nullsLast);
            default -> Comparator.comparing(Stock::getSymbol);
        };
        stocks.sort(comparator);
    }

    // ------------------------------------------------------------ admin

    public void create(User admin, Stock stock) {
        AdminService.requireAdmin(admin);
        normalise(stock);
        stockDAO.insert(stock);
        activityDAO.log(admin.getId(), "STOCK_CREATED", stock.getExchange() + ":" + stock.getSymbol());
    }

    public void update(User admin, Stock stock) {
        AdminService.requireAdmin(admin);
        normalise(stock);
        if (!stockDAO.update(stock)) {
            throw new IllegalStateException("That stock no longer exists.");
        }
        activityDAO.log(admin.getId(), "STOCK_UPDATED", stock.getExchange() + ":" + stock.getSymbol());
    }

    public void setActive(User admin, int stockId, boolean active) {
        AdminService.requireAdmin(admin);
        if (!stockDAO.updateActive(stockId, active)) {
            throw new IllegalStateException("That stock no longer exists.");
        }
        activityDAO.log(admin.getId(), active ? "STOCK_ACTIVATED" : "STOCK_DEACTIVATED", "Stock #" + stockId);
    }

    public void delete(User admin, int stockId) {
        AdminService.requireAdmin(admin);
        Stock stock = stockDAO.findById(stockId);
        if (stock == null || !stockDAO.delete(stockId)) {
            throw new IllegalStateException("That stock no longer exists.");
        }
        activityDAO.log(admin.getId(), "STOCK_DELETED", stock.getExchange() + ":" + stock.getSymbol());
    }

    /** Trims, upper-cases and validates the editable fields. */
    private static void normalise(Stock stock) {
        stock.setCompanyName(trim(stock.getCompanyName()));
        stock.setSymbol(upper(stock.getSymbol()));
        stock.setExchange(upper(stock.getExchange()));
        stock.setApiSymbol(upper(stock.getApiSymbol()));
        stock.setCurrency(upper(stock.getCurrency()));
        stock.setSector(emptyToNull(stock.getSector()));
        stock.setIndustry(emptyToNull(stock.getIndustry()));
        stock.setDescription(emptyToNull(stock.getDescription()));
        if (stock.getApiSymbol() == null || stock.getApiSymbol().isEmpty()) {
            stock.setApiSymbol(stock.getSymbol());
        }

        if (stock.getCompanyName() == null || stock.getCompanyName().length() < 2 || stock.getCompanyName().length() > 150) {
            throw new IllegalArgumentException("Enter the company name (2 to 150 characters).");
        }
        if (stock.getSymbol() == null || !SYMBOL.matcher(stock.getSymbol()).matches()) {
            throw new IllegalArgumentException("The ticker symbol may contain only letters, digits, '.', '&' and '-' (max 20).");
        }
        if (stock.getExchange() == null || !SYMBOL.matcher(stock.getExchange()).matches()) {
            throw new IllegalArgumentException("Enter a valid exchange code such as NASDAQ, NSE or BSE.");
        }
        if (!API_SYMBOL.matcher(stock.getApiSymbol()).matches()) {
            throw new IllegalArgumentException("The provider symbol may contain only letters, digits, '.', ':', '&' and '-'.");
        }
        if (stock.getCurrency() == null || !CURRENCY.matcher(stock.getCurrency()).matches()) {
            throw new IllegalArgumentException("The currency must be a 3-letter code such as USD or INR.");
        }
        if (stock.getDescription() != null && stock.getDescription().length() > 2000) {
            throw new IllegalArgumentException("The description may be at most 2000 characters.");
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Defensive copy helper for beans that keep lists in view scope. */
    public static <T> List<T> copy(List<T> list) {
        return new ArrayList<>(list);
    }
}
