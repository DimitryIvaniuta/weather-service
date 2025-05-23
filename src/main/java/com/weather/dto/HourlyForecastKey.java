package com.weather.dto;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A type-safe key for the hourly‐forecast cache, combining city + date.
 * <p>
 * Used in SpEL cache expressions as
 * {@code key = "T(com.weather.dto.HourlyForecastKey).of(#city,#date)"}.
 */
public record HourlyForecastKey(String city, LocalDate date) {

    private static final String SEPARATOR = "|";

    /**
     * Creates a new key instance after validating inputs.
     *
     * @param city  the city identifier (e.g. "London,UK"), must be non‐null/non‐blank
     * @param date  the date for which we want the forecast, must be non‐null
     * @return a validated {@code HourlyForecastKey}
     * @throws IllegalArgumentException if city is null/blank or date is null
     */
    public static HourlyForecastKey of(String city, LocalDate date) {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City must not be null or blank");
        }
        Objects.requireNonNull(date, "Date must not be null");
        return new HourlyForecastKey(city, date);
    }

    /**
     * Overrides the default Record.toString() so that, when used
     * as a cache key, it serializes to a clean string:
     *   "London,UK|2025-05-23"
     */
    @Override
    public String toString() {
        return city + SEPARATOR + date;
    }
}
