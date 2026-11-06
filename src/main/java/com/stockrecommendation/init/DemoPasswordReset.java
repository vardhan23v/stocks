package com.stockrecommendation.init;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-off recovery for the DEMO accounts on a hosted database.
 *
 * When RESET_DEMO_PASSWORDS lists emails (comma separated), each listed demo
 * account gets back the password hash it has in sample-data.sql, so the
 * passwords in the README work again. Only the demo accounts defined in that
 * file can be reset; any other email is ignored. Remove the variable after use.
 */
@Component
public class DemoPasswordReset implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoPasswordReset.class);
    private static final Pattern DEMO_ROW = Pattern.compile("'([^']+@stocksense\\.test)', '(pbkdf2\\$[^']+)'");

    private final JdbcTemplate jdbc;
    private final String emails;

    public DemoPasswordReset(JdbcTemplate jdbc, @Value("${app.reset-demo-passwords:}") String emails) {
        this.jdbc = jdbc;
        this.emails = emails;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (emails == null || emails.isBlank()) {
            return;
        }
        Map<String, String> demoHashes = demoHashes();
        for (String raw : emails.split(",")) {
            String email = raw.trim().toLowerCase(Locale.ROOT);
            String hash = demoHashes.get(email);
            if (hash == null) {
                LOG.warn("RESET_DEMO_PASSWORDS: {} is not a demo account, skipped.", email);
                continue;
            }
            int updated = jdbc.update("UPDATE users SET password_hash = ?, is_active = TRUE WHERE email = ?", hash, email);
            LOG.info("RESET_DEMO_PASSWORDS: {} {}", email, updated == 1 ? "reset to its demo password." : "not found.");
        }
    }

    /** email -> PBKDF2 hash of every demo account in sample-data.sql. */
    static Map<String, String> demoHashes() throws IOException {
        String sql;
        try (InputStream input = new ClassPathResource("sample-data.sql").getInputStream()) {
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        Map<String, String> hashes = new LinkedHashMap<>();
        Matcher matcher = DEMO_ROW.matcher(sql);
        while (matcher.find()) {
            hashes.put(matcher.group(1), matcher.group(2));
        }
        return hashes;
    }
}
