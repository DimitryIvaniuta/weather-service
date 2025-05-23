package com.weather.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Immutable DTO representing the weather forecast for a single hour.
 * <p>
 * Used both internally and as payload in REST responses.
 *
 * @param at               the timestamp of this forecast (UTC)
 * @param temperatureC     the forecast temperature in Celsius
 * @param feelsLikeC       the “feels like” temperature in Celsius
 * @param humidityPercent  the relative humidity as a percentage (0–100)
 * @param description      short textual description, e.g. “light rain”
 */
public record HourlyTemperatureDto(
        @JsonProperty("at")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime at,

        @JsonProperty("temperatureC")
        double temperatureC,

        @JsonProperty("feelsLikeC")
        double feelsLikeC,

        @JsonProperty("humidityPercent")
        int humidityPercent,

        @JsonProperty("description")
        String description

) implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A concise factory method for readability.
     *
     * @param at              timestamp of forecast
     * @param temperatureC    forecast temperature (°C)
     * @param feelsLikeC      feels‐like temperature (°C)
     * @param humidityPercent relative humidity (0–100)
     * @param description     description text
     * @return a new {@link HourlyTemperatureDto}
     */
    public static HourlyTemperatureDto of(LocalDateTime at,
                                          double temperatureC,
                                          double feelsLikeC,
                                          int humidityPercent,
                                          String description) {
        return new HourlyTemperatureDto(at, temperatureC, feelsLikeC, humidityPercent, description);
    }

    /**
     * Validates that all fields are within reasonable bounds.
     * <p>
     * Call this in constructors or builders if you want to enforce invariants.
     *
     * @throws IllegalArgumentException if any field is out of range or null
     */
    public void validate() {
        if (at == null) {
            throw new IllegalArgumentException("Timestamp 'at' must not be null");
        }
        if (humidityPercent < 0 || humidityPercent > 100) {
            throw new IllegalArgumentException("humidityPercent must be 0–100, was: " + humidityPercent);
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        // temperatureC and feelsLikeC can be negative in many locales—no further checks
    }
}
