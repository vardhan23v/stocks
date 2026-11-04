package com.stockrecommendation.bean;

import com.stockrecommendation.service.MarketDataService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * Data provider status for the page template (the "SAMPLE DATA" ribbon and
 * the footer). Application scoped: the values come from configuration.
 */
@Named("market")
@ApplicationScoped
public class MarketStatusBean {

    public boolean isDemoMode() {
        return MarketDataService.isDemoMode();
    }

    public String getProviderLabel() {
        return MarketDataService.getProviderLabel();
    }
}
