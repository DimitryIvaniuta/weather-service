package com.weather.config.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.cache.RedisCache;

import java.util.concurrent.Callable;

/**
 * A two‐level cache that delegates to an L1 CaffeineCache and an L2 RedisCache.
 * <p>
 * On {@code get} it checks L1 first, then L2 (promoting L2 hits back into L1).
 * On {@code put}/{@code evict}/{@code clear} it applies the operation to both levels.
 */
public class TwoLevelCache implements Cache {

    private final CaffeineCache l1;
    private final RedisCache   l2;

    public TwoLevelCache(CaffeineCache l1, RedisCache l2) {
        this.l1 = l1;
        this.l2 = l2;
    }

    @Override
    public String getName() {
        return l1.getName();
    }

    @Override
    public Object getNativeCache() {
        // expose both native caches if callers need direct access
        return new Object[]{ l1.getNativeCache(), l2.getNativeCache() };
    }

    @Override
    public ValueWrapper get(Object key) {
        // 1) Try L1
        ValueWrapper wrapper = l1.get(key);
        if (wrapper != null) {
            return wrapper;
        }
        // 2) Miss → try L2
        wrapper = l2.get(key);
        if (wrapper != null) {
            // Promote into L1 for faster next time
            l1.put(key, wrapper.get());
        }
        return wrapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        if (wrapper == null) {
            return null;
        }
        Object value = wrapper.get();
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new IllegalStateException(
                    "Cached value for key '" + key + "' is not of type " + type.getName()
            );
        }
        return (T) value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            @SuppressWarnings("unchecked")
            T existing = (T) wrapper.get();
            return existing;
        }
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception ex) {
            throw new ValueRetrievalException(key, valueLoader, ex);
        }
    }

    @Override
    public void put(Object key, Object value) {
        l1.put(key, value);
        l2.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper existing = get(key);
        if (existing != null) {
            return existing;
        }
        put(key, value);
        return null;
    }

    @Override
    public void evict(Object key) {
        l1.evict(key);
        l2.evict(key);
    }

    @Override
    public void clear() {
        l1.clear();
        l2.clear();
    }
}
