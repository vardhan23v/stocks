package com.stockrecommendation.filter;

import java.io.IOException;
import java.util.Set;

import com.stockrecommendation.model.User;
import com.stockrecommendation.util.SessionUtil;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Server side gate in front of every *.xhtml page.
 *
 * Two checks happen here, before JSF even starts working:
 *   1. is somebody logged in?
 *   2. does that user have the role the page requires?
 *
 * The same rules are checked again inside the service classes before any data
 * is read or changed, because a page is never the place where security lives.
 */
public class AuthFilter implements Filter {

    private static final Set<String> PUBLIC_PAGES = Set.of("/index.xhtml", "/login.xhtml", "/register.xhtml");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String contextPath = httpRequest.getContextPath();
        String path = httpRequest.getRequestURI().substring(contextPath.length());

        // Tomcat may append ;jsessionid=... - not part of the page name.
        int pathParameter = path.indexOf(';');
        if (pathParameter != -1) {
            path = path.substring(0, pathParameter);
        }
        if (path.isEmpty() || "/".equals(path)) {
            path = "/index.xhtml";   // the welcome file
        }

        // Public pages, and JSF resources such as the stylesheet: no login needed.
        if (PUBLIC_PAGES.contains(path) || path.startsWith("/jakarta.faces.resource/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        User user = session == null ? null : (User) session.getAttribute(SessionUtil.USER_KEY);

        if (user == null) {
            httpResponse.sendRedirect(contextPath + "/login.xhtml");
            return;
        }

        String requiredRole = requiredRole(path);
        if (requiredRole != null && !requiredRole.equals(user.getRole())) {
            session.setAttribute(SessionUtil.AUTH_MESSAGE_KEY, "You are not allowed to open that page.");
            httpResponse.sendRedirect(contextPath + "/dashboard.xhtml");
            return;
        }

        // Pages with personal data must not be served from the browser cache after logout.
        httpResponse.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }

    /**
     * Which role may open which page.
     *
     * @return the required role, or null when any logged-in user may open the page
     */
    private String requiredRole(String path) {
        if (path.startsWith("/admin-")) {
            return User.ROLE_ADMIN;
        }
        return null;   // dashboard, stocks, details, recommendations, watchlist, profile
    }
}
