package com.atomskills.atombackend.services;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationParser {
    private static final Pattern HHMMSS = Pattern.compile("^(\\d+):(\\d{1,2}):(\\d{1,2})$");

    private DurationParser() {}

    /**
     * Парсит строку времени в секунды.
     * Поддерживает:
     *  - "7200"           (просто секунды)
     *  - "120:30"         (минуты:секунды)
     *  - "02:30:00"       (часы:минуты:секунды)
     *  - "PT2H30M15S"     (ISO-8601)
     *  - "P1DT2H"         (ISO-8601 с днями)
     */
    public static int toSeconds(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        String s = raw.trim();

        // ISO-8601
        if (s.startsWith("P") || s.startsWith("p")) {
            try {
                return (int) Duration.parse(s).getSeconds();
            } catch (Exception ignored) {
                // падаем ниже в другие форматы
            }
        }

        // HH:MM:SS
        Matcher m = HHMMSS.matcher(s);
        if (m.matches()) {
            long h = Long.parseLong(m.group(1));
            long mi = Long.parseLong(m.group(2));
            long se = Long.parseLong(m.group(3));
            return (int) (h * 3600 + mi * 60 + se);
        }

        // MM:SS
        if (s.matches("^\\d+:\\d{1,2}$")) {
            String[] parts = s.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        }

        // Просто секунды
        if (s.matches("^\\d+$")) {
            return Integer.parseInt(s);
        }

        throw new IllegalArgumentException("Не удалось распарсить время: " + raw);
    }
}
