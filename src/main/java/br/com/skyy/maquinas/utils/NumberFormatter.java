package br.com.skyy.maquinas.utils;

import br.com.skyy.maquinas.SMaquinas;

/**
 * Delegates to sCore's NumberFormatter.
 * Keeps sMaquinas public API unchanged.
 */
public class NumberFormatter {

    private final SMaquinas plugin;

    public NumberFormatter(SMaquinas plugin) {
        this.plugin = plugin;
    }

    public String format(double value) {
        return br.com.skyy.core.utils.NumberFormatter.format(value);
    }

    public static String formatStatic(double value) {
        return br.com.skyy.core.utils.NumberFormatter.format(value);
    }

    public static String formatTime(long millis) {
        return br.com.skyy.core.utils.NumberFormatter.formatTime(millis);
    }
}
