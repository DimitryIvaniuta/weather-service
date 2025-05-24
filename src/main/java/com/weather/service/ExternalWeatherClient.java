package com.weather.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.weather.config.WeatherApiProperties;
import com.weather.dto.ForecastDto;
import com.weather.dto.HourlyTemperatureDto;
import com.weather.dto.TemperatureDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Client for OpenWeatherMap’s APIs, supporting geocoding and the OneCall hourly forecast.
 */
@Service
public class ExternalWeatherClient {

    private final WebClient webClient;

    private final WeatherApiProperties props;

    public ExternalWeatherClient(WebClient.Builder builder,
                                 WeatherApiProperties props) {
        this.webClient = builder
                .baseUrl(props.getBaseUrl())
                .build();
        this.props = props;
    }

    /**
     * Calls OpenWeatherMap’s “current weather” endpoint:
     * GET /data/2.5/weather?q={city}&units=metric&appid={key}
     *
     * @param city e.g. "London,UK"
     * @return TemperatureDto with timestamp, temp, feelsLike, humidity, description
     * @throws ResponseStatusException(503) on upstream errors
     * @throws ResponseStatusException(404) if city not found
     */
    public TemperatureDto fetchCurrent(String city) {
        WeatherResponse resp;
        try {
            resp = webClient.get()
                    .uri(uri -> uri
                            .path("/data/2.5/weather")
                            .queryParam("q", city)
                            .queryParam("units", "metric")
                            .queryParam("appid", props.getKey())
                            .build())
                    .retrieve()
                    .onStatus(
                            status -> status == HttpStatus.NOT_FOUND,
                            clientResp -> Mono.error(new ResponseStatusException(
                                    HttpStatus.NOT_FOUND, "City not found: " + city))
                    )
                    .bodyToMono(WeatherResponse.class)
                    .block(Duration.ofSeconds(2));
        } catch (WebClientResponseException e) {
            // 4xx or 5xx (other than 404) from upstream
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Upstream error fetching current weather: " + e.getStatusCode(),
                    e
            );
        } catch (Exception e) {
            // network timeout, DNS failure, etc.
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to fetch current weather for " + city,
                    e
            );
        }

        // Safety check
        if (resp == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Empty response from weather service"
            );
        }

        // Map to your DTO
        Instant timestamp = Instant.ofEpochSecond(resp.dt());
        LocalDateTime at = LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC);
        double temp = resp.main().temp();
        double feels = resp.main().feelsLike();
        int humidity = resp.main().humidity();
        String desc = Optional.ofNullable(resp.weather())
                .flatMap(list -> list.stream().findFirst())
                .map(Weather::description)
                .orElse("n/a");

        return new TemperatureDto(city, at, temp, feels, humidity, desc);
    }

    // --- JSON mapping records ---

    private static record WeatherResponse(
            long dt,
            Main main,
            List<Weather> weather
    ) {
        private static record Main(
                double temp,
                @JsonProperty("feels_like") double feelsLike,
                int humidity
        ) {
        }
    }

    private static record Weather(
            String main,
            String description
    ) {
    }

    /**
     * Fetches the 48-hour hourly forecast for the given city.
     *
     * @param city “City,CountryCode” (e.g. “London,UK”)
     * @return list of HourlyTemperatureDto (many entries, 1 per hour)
     */
    public List<HourlyTemperatureDto> fetchHourly(String city) {
        // 1) Geocode → get lat/lon
        GeoResponse geo = geocodeCity(city);

        // 2) Call One Call API for hourly data
        OneCallResponse oneCall;
        try {
            oneCall = webClient.get()
                    .uri(uri -> uri
                            .path("/data/2.5/onecall")
                            .queryParam("lat", geo.lat())
                            .queryParam("lon", geo.lon())
                            .queryParam("exclude", "current,minutely,daily,alerts")
                            .queryParam("units", "metric")
                            .queryParam("appid", props.getKey())
                            .build())

                    .retrieve()
                    .onStatus(
                            status -> status.value() == 401,
                            resp -> Mono.error(new ResponseStatusException(
                                    HttpStatus.UNAUTHORIZED,
                                    "Invalid API key for weather provider"
                            ))
                    )
                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            resp -> resp.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new ResponseStatusException(
                                                    HttpStatus.BAD_GATEWAY,
                                                    "Upstream 4xx error: "
                                                            + resp.statusCode() + " → " + body))))
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            resp -> Mono.error(new ResponseStatusException(
                                    HttpStatus.BAD_GATEWAY,
                                    "Upstream 5xx error: " + resp.statusCode())))
                    .bodyToMono(OneCallResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            // upstream returned 4xx or 5xx
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Upstream OneCall API error: " + e.getStatusCode(),
                    e
            );
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to call OneCall API",
                    e
            );
        }

        // 3) Map to your DTO
        return oneCall == null ? new ArrayList<>() : oneCall.hourly().stream()
                .map(item -> {
                    LocalDateTime at = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(item.dt()),
                            ZoneOffset.UTC
                    );
                    return HourlyTemperatureDto.of(
                            at,
                            item.temp(),
                            item.feelsLike(),
                            item.humidity(),
                            // pick first weather description
                            item.weather().stream()
                                    .findFirst()
                                    .map(OneCallResponse.Weather::description)
                                    .orElse("n/a")
                    );
                })
                .toList();
    }


    /**
     * Calls GET /data/2.5/forecast?q={city}&units=metric&appid={key}
     * @param city e.g. "London,UK"
     * @return up to 40 ForecastDto entries (3h steps for 5 days)
     */
    public List<ForecastDto> fetchFiveDayForecast(String city) {
        ForecastResponse resp;
        try {
            resp = webClient.get()
                    .uri(uri -> uri
                            .path("/data/2.5/forecast")
                            .queryParam("q",     city)
                            .queryParam("units", "metric")
                            .queryParam("appid", props.getKey())
                            .build())
                    .retrieve()
                    // handle 401 as unauthorized
                    .onStatus(s -> s.value() == 401,
                            clientResp -> Mono.error(new ResponseStatusException(
                                    HttpStatus.UNAUTHORIZED,
                                    "Invalid API key for weather provider")))
                    // other 4xx → 502
                    .onStatus(HttpStatusCode::is4xxClientError,
                            clientResp -> Mono.error(new ResponseStatusException(
                                    HttpStatus.BAD_GATEWAY,
                                    "Upstream 4xx calling forecast: " + clientResp.statusCode())))
                    // 5xx → 502
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResp -> Mono.error(new ResponseStatusException(
                                    HttpStatus.BAD_GATEWAY,
                                    "Upstream 5xx calling forecast: " + clientResp.statusCode())))
                    .bodyToMono(ForecastResponse.class)
                    .block(Duration.ofSeconds(3));
        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Error fetching 5-day forecast: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to fetch 5-day forecast for " + city, e);
        }

        if (resp == null || resp.list == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Empty forecast response for " + city);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return resp.list.stream()
                .map(item -> {
                    LocalDateTime at = LocalDateTime.parse(item.dtTxt, fmt);
                    return ForecastDto.of(
                            city,
                            at,
                            item.main.temp,
                            item.main.feelsLike,
                            item.main.humidity,
                            item.weather.getFirst().description
                    );
                })
                .toList();
    }


    private GeoResponse geocodeCity(String city) {
        GeoResponse[] geos;
        try {
            geos = webClient.get()
                    .uri(uri -> uri
                            .path("/geo/1.0/direct")
                            .queryParam("q", city)
                            .queryParam("limit", 1)
                            .queryParam("appid", props.getKey())
                            .build())
                    .retrieve()
                    .bodyToMono(GeoResponse[].class)
                    .block();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to geocode city: " + city, e
            );
        }
        if (geos == null || geos.length == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "City not found: " + city
            );
        }
        return geos[0];
    }

    // --- DTOs for upstream JSON mapping ---

    private record GeoResponse(
            double lat,
            double lon,
            String name,
            @com.fasterxml.jackson.annotation.JsonProperty("country")
            String countryCode
    ) {
    }

    private record OneCallResponse(
            List<OneCallHour> hourly
    ) {
        private record OneCallHour(
                long dt,
                double temp,
                @com.fasterxml.jackson.annotation.JsonProperty("feels_like")
                double feelsLike,
                int humidity,
                List<OneCallResponse.Weather> weather
        ) {
        }

        private record Weather(
                String main,
                String description
        ) {
        }
    }

    private record ForecastResponse(List<Item> list) {
    }

    private record Item(
            Main main,
            List<Weather> weather,
            @JsonProperty("dt_txt") String dtTxt
    ) {
    }

    private record Main(
            double temp,
            @JsonProperty("feels_like") double feelsLike,
            int humidity
    ) {
    }

}
