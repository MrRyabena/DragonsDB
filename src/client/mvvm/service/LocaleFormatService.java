package client.mvvm.service;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * Provides locale-aware formatting for numbers and dates.
 *
 * <p>Wraps NumberFormat and DateTimeFormatter to ensure consistent formatting
 * across the application based on the active locale.
 */
public class LocaleFormatService {

    public String formatNumber(Number value, Locale locale) {
        return NumberFormat.getNumberInstance(locale).format(value);
    }

    public String formatDateTime(LocalDateTime dateTime, Locale locale) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                        .withLocale(locale);
        return formatter.format(dateTime);
    }
}
