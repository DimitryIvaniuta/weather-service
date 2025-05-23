package com.weather.dto;

import java.time.LocalDateTime;


/** Immutable DTO representing a single forecast datapoint. */
public record ForecastDto(
        String city,            // e.g. "Amsterdam, NL"
        LocalDateTime at,       // timestamp (UTC)
        double temperatureC,    // °C
        double feelsLikeC,
        int humidityPercent,
        String description      // "light rain", "clear sky", etc.
) {
}
