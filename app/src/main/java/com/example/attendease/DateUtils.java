package com.example.attendease;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    // "2026-11-06" -> "Fri, 06 Nov 2026"
    public static String keyToDisplay(String key) {
        return convert(key, "EEE, dd MMM yyyy");
    }

    // "2026-11-06" -> "06 Nov"
    public static String keyToShort(String key) {
        return convert(key, "dd MMM");
    }

    private static String convert(String key, String outPattern) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            in.setTimeZone(UTC);
            Date date = in.parse(key);
            if (date == null) return key;

            SimpleDateFormat out = new SimpleDateFormat(outPattern, Locale.US);
            out.setTimeZone(UTC);
            return out.format(date);
        } catch (ParseException e) {
            return key; // show the raw key if it can't be parsed
        }
    }
}