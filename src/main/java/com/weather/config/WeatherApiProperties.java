package com.weather.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "weather.api")
public class WeatherApiProperties {
    /**
     * The OpenWeather API key.
     */
    private String key;

    /**
     * The base URL of the external weather service.
     */
    private String baseUrl;
}
