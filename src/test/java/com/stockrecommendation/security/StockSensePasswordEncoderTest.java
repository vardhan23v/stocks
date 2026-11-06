package com.stockrecommendation.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** The Spring Security encoder must accept the PBKDF2 hashes already stored in MySQL. */
class StockSensePasswordEncoderTest {

    private final StockSensePasswordEncoder encoder = new StockSensePasswordEncoder();

    @Test
    void encodesWithARandomSaltAndMatches() {
        String first = encoder.encode("Secret123");
        String second = encoder.encode("Secret123");
        assertTrue(first.startsWith("pbkdf2$"));
        assertNotEquals(first, second);
        assertTrue(encoder.matches("Secret123", first));
        assertFalse(encoder.matches("Secret124", first));
    }

    @Test
    void acceptsTheDemoAdminHashFromSampleData() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/sample-data.sql"));
        Matcher matcher = Pattern.compile("'admin@stocksense\\.test', '(pbkdf2\\$[^']+)'").matcher(sql);
        assertTrue(matcher.find(), "admin row not found in sample-data.sql");
        assertTrue(encoder.matches("Admin@123", matcher.group(1)));
        assertFalse(encoder.matches("admin@123", matcher.group(1)));
    }
}
