package com.weather.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableCaching
public class CacheConfig {

    // the two cache names we use in @Cacheable
    private static final String CURRENT = "currentTemp";
    private static final String HOURLY  = "hourlyForecast";

    /**
     * L1: In‐JVM Caffeine cache manager.
     * Defines exactly two caches (currentTemp, hourlyForecast).
     */
    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager(CURRENT, HOURLY);
        mgr.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMinutes(5))   // for currentTemp
                        .maximumSize(5_000)
        );
        // We’ll only use this manager for currentTemp → override TTL below
        // and create a separate spec for hourlyForecast if you need different L1 TTLs.
        return mgr;
    }

    /**
     * L2: Redis cache manager.
     * Uses the same cache names, but with longer, per-cache TTLs.
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory cf) {
        // default TTL (won’t actually be used—each cache has its own)
        RedisCacheConfiguration defaultCfg = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(5));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(defaultCfg)
                .withCacheConfiguration(CURRENT,
                        defaultCfg.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration(HOURLY,
                        defaultCfg.entryTtl(Duration.ofHours(1)))
                .build();
    }

    /**
     * Compose the two managers: first try Caffeine (L1), then fall back to Redis (L2).
     */
    @Bean
    @Primary
    public CacheManager cacheManager(
            CaffeineCacheManager caffeineCacheManager,
            RedisCacheManager      redisCacheManager) {
        CompositeCacheManager mgr = new CompositeCacheManager(
                caffeineCacheManager,
                redisCacheManager
        );
        // if neither has the cache, don’t create an empty one automatically:
        mgr.setFallbackToNoOpCache(false);
        return mgr;
    }
}
