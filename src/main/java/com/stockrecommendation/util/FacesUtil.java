package com.stockrecommendation.util;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

/**
 * Small helpers around FacesMessage, so the beans do not repeat the same
 * four lines every time they want to show something to the user.
 */
public final class FacesUtil {

    private FacesUtil() {
        // utility class: never instantiated
    }

    /** Shows a green "everything worked" message. */
    public static void addInfo(String message) {
        add(FacesMessage.SEVERITY_INFO, message);
    }

    /** Shows an amber warning. */
    public static void addWarn(String message) {
        add(FacesMessage.SEVERITY_WARN, message);
    }

    /** Shows a red error message. */
    public static void addError(String message) {
        add(FacesMessage.SEVERITY_ERROR, message);
    }

    /**
     * Adds a global message: it is displayed by <h:messages globalOnly="true"/>
     * in the page template instead of next to one input field.
     */
    private static void add(FacesMessage.Severity severity, String message) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.addMessage(null, new FacesMessage(severity, message, message));
        }
    }

    /**
     * Keeps the current messages for the next request, so a message added
     * just before returning "page?faces-redirect=true" survives the redirect.
     */
    public static void keepMessagesForRedirect() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.getExternalContext().getFlash().setKeepMessages(true);
        }
    }

    /** Reads one request parameter, or null. */
    public static String param(String name) {
        FacesContext context = FacesContext.getCurrentInstance();
        return context == null ? null : context.getExternalContext().getRequestParameterMap().get(name);
    }
}
