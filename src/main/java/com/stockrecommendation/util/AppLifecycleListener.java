package com.stockrecommendation.util;

import com.stockrecommendation.service.MarketDataService;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/** Stops the market data background thread when Tomcat undeploys the application. */
@WebListener
public class AppLifecycleListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        MarketDataService.shutdown();
    }
}
