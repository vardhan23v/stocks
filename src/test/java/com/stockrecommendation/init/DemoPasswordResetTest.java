package com.stockrecommendation.init;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.stockrecommendation.util.PasswordUtil;

/** The reset only knows the four demo accounts, with the README passwords and their roles. */
class DemoPasswordResetTest {

    @Test
    void readsTheFourDemoAccountsAndTheirPasswordsMatch() throws Exception {
        Map<String, DemoPasswordReset.DemoAccount> accounts = DemoPasswordReset.demoAccounts();
        assertEquals(4, accounts.size());
        DemoPasswordReset.DemoAccount admin = accounts.get("admin@stocksense.test");
        assertEquals("ADMIN", admin.role());
        assertEquals("Asha Rao (Admin)", admin.name());
        assertTrue(PasswordUtil.matches("Admin@123", admin.passwordHash()));
        assertEquals("USER", accounts.get("priya@stocksense.test").role());
        assertTrue(PasswordUtil.matches("User@1234", accounts.get("priya@stocksense.test").passwordHash()));
    }
}
