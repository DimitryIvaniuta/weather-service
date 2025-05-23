package com.weather.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix="app.cors")
public class CorsProperties {
    /**
     * Origins allowed for CORS.
     */
    private List<String> allowedOrigins;
}
