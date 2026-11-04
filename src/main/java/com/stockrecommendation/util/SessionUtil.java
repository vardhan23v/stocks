package com.stockrecommendation.util;

import java.util.Map;

import com.stockrecommendation.model.User;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Reads and writes the logged-in user in the HTTP session.
 *
 * Both the JSF beans and the AuthFilter use this class, so the session
 * attribute name ("currentUser") exists in exactly one place.
 */
public final class SessionUtil {

    /** Session attribute holding the logged-in User object. */
    public static final String USER_KEY = "currentUser";

    /** Session attribute holding a one-time message set by the AuthFilter. */
    public static final String AUTH_MESSAGE_KEY = "authMessage";

    private SessionUtil() {
        // utility class: never instantiated
    }

    private static Map<String, Object> sessionMap() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context == null ? null : context.getExternalContext().getSessionMap();
    }

    /** @return the logged-in user, or null when nobody is logged in */
    public static User getCurrentUser() {
        Map<String, Object> session = sessionMap();
        return session == null ? null : (User) session.get(USER_KEY);
    }

    /**
     * Stores the user in the session - called by LoginBean after a successful
     * login. The old session is replaced by a new one first, so a session id
     * that existed before the login can never be used afterwards (session
     * fixation).
     */
    public static void setCurrentUser(User user) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return;
        }
        Object request = context.getExternalContext().getRequest();
        if (request instanceof HttpServletRequest httpRequest && httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }
        context.getExternalContext().getSessionMap().put(USER_KEY, user);
    }

    /** @return true when somebody is logged in with the given role */
    public static boolean hasRole(String role) {
        User user = getCurrentUser();
        return user != null && role != null && role.equals(user.getRole());
    }

    /** Invalidates the whole session - called by LoginBean.logout(). */
    public static void clearSession() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return;
        }
        Object session = context.getExternalContext().getSession(false);
        if (session instanceof HttpSession httpSession) {
            httpSession.invalidate();
        }
    }

    /** Returns the pending message and removes it, so it is shown only once. */
    public static String consumeAuthMessage() {
        Map<String, Object> session = sessionMap();
        if (session == null) {
            return null;
        }
        Object message = session.remove(AUTH_MESSAGE_KEY);
        return message == null ? null : message.toString();
    }
}
