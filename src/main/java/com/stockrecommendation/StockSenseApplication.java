package com.stockrecommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * StockSense - Spring Boot entry point.
 *
 * Spring MVC controllers (web) -> services -> JdbcTemplate DAOs -> MySQL,
 * JSP views (src/main/webapp/WEB-INF/jsp), Spring Security for login and roles.
 *
 * Packaged as a WAR: run it with "java -jar stock-recommendation.war" (embedded
 * Tomcat), or drop it into an external Tomcat 10.1, which uses configure() below.
 */
@SpringBootApplication
public class StockSenseApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(StockSenseApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(StockSenseApplication.class, args);
    }
}
