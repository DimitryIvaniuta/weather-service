package com.weather.service;

import com.weather.dto.ForecastDto;
import com.weather.dto.HourlyTemperatureDto;
import com.weather.dto.TemperatureDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

import static com.weather.config.cache.CacheConfig.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class WeatherService {

    private static final String RESILIENCE_INSTANCE = "weatherService";

    private final ExternalWeatherClient client;


    /**
     * Fetch the current temperature for a given city.
     * 1. Retry up to 3 times (500ms→1s→2s) on failure.
     * 2. Circuit opens after 2 failed calls, for 10s.
     * 3. Fallback returns 503.
     * <ul>
     *   <li>Only USER or ADMIN may call.</li>
     *   <li>Results cached (L1/L2) under cache name "currentTemp" with key city|date</li>
     *   <li>Retries up to 3× and opens circuit after 2 failures (see application.yml)</li>
     * </ul>
     *
     * @param city non-blank city identifier (e.g. "London,UK")
     * @param date the request date (used in cache key; typically today)
     * @return the current {@link TemperatureDto}
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Cacheable(cacheNames = CURRENT_CACHE, key = "#city")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "currentFallback")
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "currentFallback")
    public TemperatureDto getCurrentTemperature(String city, LocalDate date) {
        log.info("Fetching current temperature for city='{}' date='{}'", city, date);
        TemperatureDto dto = client.fetchCurrent(city);
        log.debug("Received current temperature: {}", dto);
        return dto;
    }

    /**
     * Fallback invoked if retries are exhausted or circuit is open.
     */
    private TemperatureDto currentFallback(String city,
                                           LocalDate date,
                                           Throwable ex) {
        log.warn("Fallback for getCurrentTemperature({}, {}) → {}", city, date, ex.toString());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service unavailable fetching current weather for " + city,
                ex
        );
    }

    /**
     * Hourly forecast for a given date.
     * Same resilience patterns/fallback.
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Cacheable(cacheNames = HOURLY_CACHE,
            key = "T(com.weather.dto.HourlyForecastKey).of(#city,#date)")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "forecastFallback")
    @CircuitBreaker(
            name = RESILIENCE_INSTANCE,
            fallbackMethod = "forecastFallback"
    )
    public List<HourlyTemperatureDto> getHourlyForecast(String city, LocalDate date) {
        log.info("Fetching hourly forecast for city='{}' on date='{}'", city, date);
        validateDateRange(date);

        // 1) Fetch the complete hourly series (e.g. 48 entries)
        List<HourlyTemperatureDto> all = client.fetchHourly(city);

        // 2) Filter to the requested date and sort by timestamp
        List<HourlyTemperatureDto> daily = all.stream()
                .filter(h -> h.at().toLocalDate().equals(date))
                .sorted(Comparator.comparing(HourlyTemperatureDto::at))
                .toList();

        log.debug("Hourly forecast for {} on {}: {} entries", city, date, daily.size());
        return daily;
    }

    /**
     * Validates that the given date is within the next five days (inclusive)
     * from “today” (UTC). Throws a 400 Bad Request if not.
     *
     * @param date the date to validate (must not be null)
     * @throws ResponseStatusException with HttpStatus.BAD_REQUEST on invalid input
     */
    private void validateDateRange(LocalDate date) {
        if (date == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Required request parameter 'date' is missing or null"
            );
        }

        // Use UTC “today” to avoid timezone drift
        LocalDate today   = LocalDate.now(ZoneOffset.UTC);
        LocalDate maxDate = today.plusDays(5);

        if (date.isBefore(today) || date.isAfter(maxDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format(
                            "Parameter 'date' must be between %s and %s (inclusive)",
                            today,
                            maxDate
                    )
            );
        }
    }

    public List<HourlyTemperatureDto> forecastFallback(String city, LocalDate date, Throwable ex) {
        log.warn("Fallback for getHourlyForecast(city={}, date={}) → {}", city, date, ex.toString());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to fetch hourly forecast for " + city + " on " + date,
                ex
        );
    }

    /**
     * @param city e.g. "Warsaw,PL"
     * @return up to 40 forecast points, each a 3-hour step
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Cacheable(cacheNames = FIVE_DAYS_CACHE, key = "#city")
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "fiveDayFallback")
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "fiveDayFallback")
    public List<ForecastDto> getFiveDayForecast(String city) {
        log.info("Fetching 5-day forecast for '{}'", city);
        List<ForecastDto> batch = client.fetchFiveDayForecast(city);
        log.debug("Fetched {} points for '{}'", batch.size(), city);
        return batch;
    }

    @SuppressWarnings("unused") // used reflectively
    private List<ForecastDto> fiveDayFallback(String city, Throwable ex) {
        log.warn("Fallback for 5-day forecast '{}': {}", city, ex.toString());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to fetch 5-day forecast for " + city,
                ex
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(cacheNames = {CURRENT_CACHE, HOURLY_CACHE}, allEntries = true)
    public void purgeCache() {
        // no implementation needed; annotation handles eviction
    }

}
