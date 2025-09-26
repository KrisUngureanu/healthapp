package com.sportfd.healthapp.util;

import java.time.*;

public final class TimeUtil {
    private TimeUtil() {}

    /** Превращает LocalDateTime + offsetMinutes (минуты) в OffsetDateTime.
     * Если offsetMinutes=null — берём UTC. */
    public static OffsetDateTime toOffset(LocalDateTime ldt, Integer offsetMinutes) {
        if (ldt == null) return null;
        ZoneOffset off = offsetMinutes != null
                ? ZoneOffset.ofTotalSeconds(offsetMinutes * 60)
                : ZoneOffset.UTC;
        return ldt.atOffset(off);
    }


    public static OffsetDateTime parseFlexible(String text, Integer offsetMinutes) {
        if (text == null || text.isBlank()) return null;
        try {
            return OffsetDateTime.parse(text); // с Z/±HH:MM
        } catch (Exception ignored) {
            LocalDateTime ldt = LocalDateTime.parse(text);
            return toOffset(ldt, offsetMinutes);
        }
    }


    public static OffsetDateTime parseFlexibleAdditional(String text, Integer offsetMinutes) {
        if (text == null || text.isBlank()) return null;
        try {

            return java.time.OffsetDateTime.parse(text);
        } catch (Exception ignore1) {
            try {

                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(text);
                java.time.ZoneOffset off = offsetMinutes != null
                        ? java.time.ZoneOffset.ofTotalSeconds(offsetMinutes * 60)
                        : java.time.ZoneOffset.UTC;
                return ldt.atOffset(off);
            } catch (Exception ignore2) {

                java.time.LocalDate d = java.time.LocalDate.parse(text);
                return d.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
            }
        }
    }
}