package com.stockrecommendation.security;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.stockrecommendation.dao.UserDAO;
import com.stockrecommendation.model.User;

/** Loads a user by email for Spring Security's login check. Deactivated users are disabled. */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserDAO userDAO;

    public AppUserDetailsService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = email == null ? null : userDAO.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (user == null) {
            throw new UsernameNotFoundException("Invalid email or password.");
        }
        return new AppUser(user);
    }
}
