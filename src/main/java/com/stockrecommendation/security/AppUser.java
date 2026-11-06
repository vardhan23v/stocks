package com.stockrecommendation.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.stockrecommendation.model.User;

/**
 * The logged-in user as Spring Security sees it. It wraps the application's
 * own User, which controllers get with @AuthenticationPrincipal AppUser.
 */
public class AppUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final User user;
    private final String passwordHash;

    public AppUser(User user) {
        this.user = user;
        this.passwordHash = user.getPasswordHash();
        user.setPasswordHash(null);   // the hash is not kept on the User in the session
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
