package com.stockrecommendation.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Number and date formatting for the Thymeleaf templates, used as
 * ${@fmt.money(stock.latestPrice)}. Null values print as an em dash.
 */
@Component("fmt")
public class Formats {

    private static final String DASH = "—";
    private static final DecimalFormatSymbols US = DecimalFormatSymbols.getInstance(Locale.US);

    public String money(BigDecimal value) {
        return format(value, "#,##0.00");
    }

    public String decimal(BigDecimal value, String pattern) {
        return format(value, pattern);
    }

    public String signed(BigDecimal value) {
        return format(value, "+#,##0.00;-#,##0.00");
    }

    public String percent(BigDecimal value) {
        return value == null ? DASH : format(value, "+0.00;-0.00") + "%";
    }

    public String whole(Number value) {
        return value == null ? DASH : new DecimalFormat("#,##0", US).format(value);
    }

    public String date(LocalDateTime value) {
        return value == null ? DASH : value.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
    }

    public String date(LocalDate value) {
        return value == null ? DASH : value.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH));
    }

    public String dateTime(LocalDateTime value) {
        return value == null ? DASH : value.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH));
    }

    public String time(LocalDateTime value) {
        return value == null ? DASH : value.format(DateTimeFormatter.ofPattern("dd MMM, HH:mm:ss", Locale.ENGLISH));
    }

    /** "AR" for "Asha Rao (Admin)": first letters of the first and last word that start with a letter. */
    public String initials(String name) {
        if (name == null) {
            return "?";
        }
        String[] parts = java.util.Arrays.stream(name.trim().split("\\s+"))
                .filter(part -> !part.isEmpty() && Character.isLetter(part.charAt(0)))
                .toArray(String[]::new);
        if (parts.length == 0) {
            return "?";
        }
        return (parts[0].substring(0, 1) + (parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "")).toUpperCase();
    }

    private static String format(BigDecimal value, String pattern) {
        if (value == null) {
            return DASH;
        }
        DecimalFormat format = new DecimalFormat(pattern, US);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }
}
