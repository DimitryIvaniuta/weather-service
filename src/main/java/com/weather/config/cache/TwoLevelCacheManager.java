package com.weather.config.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;
import java.util.Collection;

/**
 * A CacheManager that returns a TwoLevelCache for each cache name.
 */
public class TwoLevelCacheManager implements CacheManager {

    private final CaffeineCacheManager l1Manager;
    private final RedisCacheManager   l2Manager;

    public TwoLevelCacheManager(CaffeineCacheManager l1, RedisCacheManager l2) {
        this.l1Manager = l1;
        this.l2Manager = l2;
    }

    @Override
    public Cache getCache(String name) {
        // both must have that cache defined
        CaffeineCache l1 = (CaffeineCache) l1Manager.getCache(name);
        RedisCache   l2 = (RedisCache)   l2Manager.getCache(name);
        if (l1 == null || l2 == null) {
            return null;
        }
        return new TwoLevelCache(l1, l2);
    }

    @Override
    public Collection<String> getCacheNames() {
        // use L1's names (assumed same set in L2)
        return l1Manager.getCacheNames();
    }
}
