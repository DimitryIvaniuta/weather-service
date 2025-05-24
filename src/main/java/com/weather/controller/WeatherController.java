package com.weather.controller;

import com.weather.dto.ForecastDto;
import com.weather.dto.HourlyTemperatureDto;
import com.weather.dto.TemperatureDto;
import com.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;

/**
 * REST controller exposing weather‐lookup endpoints.
 * <p>
 * • GET  /weather/current?city={city}&date={YYYY-MM-DD}
 * • GET  /weather/hourly?city={city}&date={YYYY-MM-DD}
 * • POST /weather/cache/purge  (ADMIN only)
 */
@RestController
@RequestMapping("/weather")
@Validated
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * Returns the current temperature for the given city & date.
     *
     * @param city non‐blank city identifier, e.g. "London,UK"
     * @param date ISO date (UTC) for which to fetch the “current” temperature
     * @return a {@link TemperatureDto}
     */
    @GetMapping("/current")
    public TemperatureDto getCurrentTemperature(
            @RequestParam("city") @NotBlank String city,
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return weatherService.getCurrentTemperature(city, date);
    }

    /**
     * Returns the hourly forecast for the given city & date.
     *
     * @param city non‐blank city identifier, e.g. "London,UK"
     * @param date ISO date (UTC) for which to fetch up to 24 hourly entries
     * @return list of {@link HourlyTemperatureDto}, sorted by hour
     */
    @GetMapping("/hourly")
    public List<HourlyTemperatureDto> getHourlyForecast(
            @RequestParam("city") @NotBlank String city,
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return weatherService.getHourlyForecast(city, date);
    }

    @GetMapping("/forecast")
    public List<ForecastDto> get5DayForecast(@RequestParam("city") String city) {
        return weatherService.getFiveDayForecast(city);
    }
    /**
     * Clears all entries in both the current‐temperature and hourly‐forecast caches.
     * <p>
     * Access restricted to users with ROLE_ADMIN.
     *
     * @return HTTP 204 No Content on success
     */
    @PostMapping("/cache/purge")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> purgeCache() {
        weatherService.purgeCache();
        return ResponseEntity.noContent().build();
    }


}
