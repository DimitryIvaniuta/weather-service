package com.weather.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.time.LocalDateTime;

import java.io.Serializable;
import java.util.Objects;

/**
 * A single 3-hour forecast point for a city.
 */
public record ForecastDto(

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

    @Serial
    private static final long serialVersionUID = 1L;

    public static ForecastDto of(String city,
                                 LocalDateTime at,
                                 double temperatureC,
                                 double feelsLikeC,
                                 int humidityPercent,
                                 String description) {
        ForecastDto dto = new ForecastDto(city, at, temperatureC, feelsLikeC, humidityPercent, description);
        dto.validate();
        return dto;
    }

    private void validate() {
        Objects.requireNonNull(city, "city must not be null");
        if (city.isBlank()) throw new IllegalArgumentException("city must not be blank");
        Objects.requireNonNull(at,   "at must not be null");
        if (humidityPercent < 0 || humidityPercent > 100)
            throw new IllegalArgumentException("humidityPercent must be 0–100");
        Objects.requireNonNull(description, "description must not be null");
        if (description.isBlank()) throw new IllegalArgumentException("description must not be blank");
    }
}
