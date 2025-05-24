package com.weather.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for inspecting and clearing both the L1 (Caffeine) and L2 (Redis) caches.
 */
@Service
@RequiredArgsConstructor
public class CacheService {

    private final CaffeineCacheManager caffeineManager;

    private final StringRedisTemplate redisTemplate;

    /**
     * Returns all entries in each Caffeine cache.
     *
     * @return Map&lt;cacheName, Map&lt;keyString, rawValue&gt;&gt;
     */
    public Map<String, Map<String, Object>> getL1CacheContents() {
        Map<String, Map<String, Object>> cacheMap = new LinkedHashMap<>();

        for(String cacheName : caffeineManager.getCacheNames()) {
            org.springframework.cache.Cache springCache = caffeineManager.getCache(cacheName);
            if(springCache instanceof CaffeineCache cc){
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = cc.getNativeCache();
                Map<String, Object> entries = nativeCache.asMap().entrySet().stream()
                        .collect(Collectors.toMap(e -> Objects.toString(e.getKey(), "null"),
                                Map.Entry::getValue,
                                (a, b) -> b,
                                LinkedHashMap::new
                                ));
                cacheMap.put(cacheName, entries);
            }
        }
        return cacheMap;
    }

    /**
     * Returns all entries in each Redis cache.
     *
     * @return Map&lt;cacheName, Map&lt;keyString, valueJson&gt;&gt;
     */
    public Map<String, Map<String, String>> getL2CacheContents() {
        Map<String, Map<String, String>> cacheMap = new LinkedHashMap<>();
        for(String cacheName : caffeineManager.getCacheNames()) {
            String pattern = cacheName + "::" + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            if(keys == null || keys.isEmpty()){
                cacheMap.put(cacheName, Collections.emptyMap());
            } else {
                Map<String, String> entries = new LinkedHashMap<>();
                for(String fullKey : keys){
                    String plainKey = fullKey.substring(cacheName.length() + 2);
                    String val = redisTemplate.opsForValue().get(fullKey);
                    entries.put(plainKey, val);
                }
                cacheMap.put(cacheName, entries);
            }
        }
        return cacheMap;
    }

    /**
     * Clears all entries from every L1 and L2 cache.
     */
    public void purgeAllCaches() {
        // L1
        for (String cacheName : caffeineManager.getCacheNames()) {
            org.springframework.cache.Cache springCache = caffeineManager.getCache(cacheName);
            if (springCache != null) {
                springCache.clear();
            }
        }
        // L2
        for (String cacheName : caffeineManager.getCacheNames()) {
            String pattern = cacheName + "::" + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }
}
