package com.stockrecommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * StockSense - Spring Boot entry point.
 *
 * Spring MVC controllers (web) -> services -> JdbcTemplate DAOs -> MySQL,
 * Thymeleaf views, Spring Security for login and roles.
 */
@SpringBootApplication
public class StockSenseApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockSenseApplication.class, args);
    }
}
