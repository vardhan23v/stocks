package com.stockrecommendation.bean;

import java.io.Serializable;
import java.util.List;

import com.stockrecommendation.model.Stock;
import com.stockrecommendation.service.StockService;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.FacesUtil;
import com.stockrecommendation.util.SessionUtil;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/** Backing bean of admin-stocks.xhtml: create, edit, (de)activate and delete stocks. */
@Named("adminStockBean")
@ViewScoped
public class AdminStockBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final StockService stockService = new StockService();

    private String q;
    private List<Stock> stocks = List.of();

    /** The stock in the form: a new one, or a copy of the one being edited. */
    private Stock form;
    private boolean editing;

    @PostConstruct
    public void init() {
        reload();
    }

    public void search() {
        reload();
    }

    private void reload() {
        try {
            stocks = stockService.findAllForAdmin(q);
        } catch (DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
    }

    public void newStock() {
        form = new Stock();
        editing = false;
    }

    public void edit(int stockId) {
        Stock stock = stockService.getStock(stockId);
        if (stock == null) {
            FacesUtil.addError("That stock no longer exists.");
            return;
        }
        form = stock;
        editing = true;
    }

    public void cancel() {
        form = null;
        editing = false;
    }

    public void save() {
        try {
            if (editing) {
                stockService.update(SessionUtil.getCurrentUser(), form);
                FacesUtil.addInfo(form.getSymbol() + " was updated.");
            } else {
                stockService.create(SessionUtil.getCurrentUser(), form);
                FacesUtil.addInfo(form.getSymbol() + " was added. It has no price data until a sync runs.");
            }
            form = null;
            editing = false;
            reload();
        } catch (SecurityException | IllegalArgumentException | IllegalStateException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
    }

    public void changeActive(int stockId, boolean active) {
        try {
            stockService.setActive(SessionUtil.getCurrentUser(), stockId, active);
            FacesUtil.addInfo(active ? "The stock was activated." : "The stock was deactivated and is hidden from users.");
        } catch (SecurityException | IllegalStateException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
        reload();
    }

    public void delete(int stockId) {
        try {
            stockService.delete(SessionUtil.getCurrentUser(), stockId);
            FacesUtil.addInfo("The stock and all its stored data were deleted.");
            if (form != null && form.getId() == stockId) {
                cancel();
            }
        } catch (SecurityException | IllegalStateException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
        reload();
    }

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }

    public List<Stock> getStocks() { return stocks; }
    public Stock getForm() { return form; }
    public boolean isEditing() { return editing; }
}
