package com.stockrecommendation.init;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.stockrecommendation.util.PasswordUtil;

/** The reset can only use the four demo hashes, and they match the README passwords. */
class DemoPasswordResetTest {

    @Test
    void readsTheFourDemoAccountsAndTheirPasswordsMatch() throws Exception {
        Map<String, String> hashes = DemoPasswordReset.demoHashes();
        assertEquals(4, hashes.size());
        assertTrue(PasswordUtil.matches("Admin@123", hashes.get("admin@stocksense.test")));
        assertTrue(PasswordUtil.matches("User@1234", hashes.get("priya@stocksense.test")));
    }
}
