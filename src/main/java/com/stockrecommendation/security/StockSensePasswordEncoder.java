package com.stockrecommendation.security;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.stockrecommendation.util.PasswordUtil;

/**
 * Spring Security PasswordEncoder over the application's PBKDF2-SHA256 format
 * (pbkdf2$iterations$salt$hash, see PasswordUtil). Using the same format keeps
 * every password already stored in MySQL valid after the move to Spring.
 */
public class StockSensePasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        return PasswordUtil.hash(rawPassword.toString());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return rawPassword != null && PasswordUtil.matches(rawPassword.toString(), encodedPassword);
    }
}
