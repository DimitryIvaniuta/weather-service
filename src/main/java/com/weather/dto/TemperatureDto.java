package com.weather.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Immutable Data Transfer Object representing the current weather conditions
 * for a specific city at a specific timestamp.
 *
 * @param city            the city identifier, e.g. "London,UK"
 * @param at              the timestamp of the observation (UTC)
 * @param temperatureC    the temperature in degrees Celsius
 * @param feelsLikeC      the “feels like” temperature in degrees Celsius
 * @param humidityPercent the relative humidity as a percentage (0–100)
 * @param description     a brief textual description, e.g. “light rain”
 */
public record TemperatureDto(

        @JsonProperty("city")
        String city,

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
     * Creates a new {@code TemperatureDto}, validating arguments.
     *
     * @param city            non-null, non-blank city identifier
     * @param at              non-null timestamp (UTC)
     * @param temperatureC    any double (will typically be in a meteorological range)
     * @param feelsLikeC      any double
     * @param humidityPercent 0–100 inclusive
     * @param description     non-null, non-blank weather description
     * @return a validated {@link TemperatureDto}
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static TemperatureDto of(String city,
                                    LocalDateTime at,
                                    double temperatureC,
                                    double feelsLikeC,
                                    int humidityPercent,
                                    String description) {
        TemperatureDto dto = new TemperatureDto(city, at, temperatureC, feelsLikeC, humidityPercent, description);
        dto.validate();
        return dto;
    }

    /**
     * Validates that all fields meet their domain constraints.
     *
     * @throws IllegalArgumentException if any field is null, blank, or out of expected range
     */
    public void validate() {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("city must not be null or blank");
        }
        if (at == null) {
            throw new IllegalArgumentException("at timestamp must not be null");
        }
        if (humidityPercent < 0 || humidityPercent > 100) {
            throw new IllegalArgumentException("humidityPercent must be between 0 and 100");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be null or blank");
        }
        // temperatureC and feelsLikeC may be any double value (including negatives)
    }

    @Override
    public String toString() {
        return "TemperatureDto[" +
                "city='" + city + '\'' +
                ", at=" + at +
                ", temperatureC=" + temperatureC +
                ", feelsLikeC=" + feelsLikeC +
                ", humidityPercent=" + humidityPercent +
                ", description='" + description + '\'' +
                ']';
    }
}
