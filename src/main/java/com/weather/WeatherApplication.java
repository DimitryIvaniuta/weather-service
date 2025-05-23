package com.weather;

import com.weather.config.CorsProperties;
import com.weather.config.WeatherApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableConfigurationProperties({
        WeatherApiProperties.class,
        CorsProperties.class
})
@SpringBootApplication
@EnableCaching
@EnableMethodSecurity
public class WeatherApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherApplication.class, args);
    }

}
