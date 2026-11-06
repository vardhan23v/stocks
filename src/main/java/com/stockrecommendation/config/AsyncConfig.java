package com.stockrecommendation.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * One background thread for "Sync all stale stocks", so an admin page never
 * waits minutes for the provider. Spring stops it when the application stops.
 */
@Configuration
@EnableConfigurationProperties(MarketProperties.class)
public class AsyncConfig {

    @Bean(name = "marketDataExecutor")
    public ThreadPoolTaskExecutor marketDataExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("market-data-sync-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
