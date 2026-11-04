package com.stockrecommendation.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** One watchlist entry: a user follows a stock. The stock carries its latest quote. */
public class Watchlist implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private int stockId;
    private LocalDateTime addedAt;
    private Stock stock;

    public Watchlist() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getStockId() { return stockId; }
    public void setStockId(int stockId) { this.stockId = stockId; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public Stock getStock() { return stock; }
    public void setStock(Stock stock) { this.stock = stock; }
}
