package com.stockrecommendation.init;

import java.sql.Connection;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

/**
 * Creates the tables and loads the sample data on an EMPTY database.
 *
 * Only when app.db-init is true (DB_INIT=true) and the users table does not
 * exist yet - so it is safe to leave on: a database that already has data is
 * never touched. Meant for hosts such as Railway, where MySQL is reachable
 * only from inside the platform.
 */
@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final boolean enabled;

    public DatabaseInitializer(DataSource dataSource, JdbcTemplate jdbc, @Value("${app.db-init:false}") boolean enabled) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            return;
        }
        Integer tables = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = 'stock_recommendation_db' AND table_name = 'users'", Integer.class);
        if (tables != null && tables > 0) {
            LOG.info("DB_INIT: tables already exist, nothing to do.");
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            // One connection for both scripts: sample-data.sql uses session variables (@stock_id).
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("sample-data.sql"));
        }
        LOG.info("DB_INIT: schema and sample data loaded.");
    }
}
