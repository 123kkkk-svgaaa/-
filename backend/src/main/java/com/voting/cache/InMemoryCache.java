package com.voting.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory cache replacing Redis.
 * Uses ConcurrentHashMap for thread-safe operations.
 * Background cleaner thread periodically purges expired entries.
 */
@Component
public class InMemoryCache {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCache.class);

    private final ConcurrentMap<String, CacheEntry<String>> store = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConcurrentHashMap<String, String>> hashStore = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> setStore = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cache-cleaner");
        t.setDaemon(true);
        return t;
    });

    // Default TTL for hash/set entries: 6 hours (poll lifetime window)
    private static final long HASH_TTL_MS = TimeUnit.HOURS.toMillis(6);
    private final ConcurrentMap<String, Long> hashExpiry = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> setExpiry = new ConcurrentHashMap<>();

    @PostConstruct
    public void startCleaner() {
        cleaner.scheduleWithFixedDelay(() -> {
            try {
                int removed = 0;
                // String store (explicit TTL)
                for (var entry : store.entrySet()) {
                    if (entry.getValue().isExpired()) {
                        store.remove(entry.getKey());
                        removed++;
                    }
                }
                // Hash store (auto-expire after 6h)
                long now = System.currentTimeMillis();
                for (var entry : hashExpiry.entrySet()) {
                    if (now > entry.getValue()) {
                        hashStore.remove(entry.getKey());
                        hashExpiry.remove(entry.getKey());
                        removed++;
                    }
                }
                // Set store (auto-expire after 6h)
                for (var entry : setExpiry.entrySet()) {
                    if (now > entry.getValue()) {
                        setStore.remove(entry.getKey());
                        setExpiry.remove(entry.getKey());
                        removed++;
                    }
                }
                if (removed > 0) {
                    log.debug("Cache cleaner removed {} expired entries", removed);
                }
            } catch (Exception e) {
                log.warn("Cache cleaner error: {}", e.getMessage());
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void shutdown() {
        cleaner.shutdownNow();
    }

    // === String operations ===

    public void set(String key, String value, long ttl, TimeUnit unit) {
        long expireAt = System.currentTimeMillis() + unit.toMillis(ttl);
        store.put(key, new CacheEntry<>(value, expireAt));
    }

    public String get(String key) {
        CacheEntry<String> entry = store.get(key);
        if (entry == null) return null;
        if (entry.isExpired()) {
            store.remove(key);
            return null;
        }
        return entry.value;
    }

    public void delete(String key) {
        store.remove(key);
    }

    // === Hash operations ===

    public void hset(String key, String hashKey, String value) {
        hashStore.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(hashKey, value);
        hashExpiry.put(key, System.currentTimeMillis() + HASH_TTL_MS);
    }

    public String hget(String key, String hashKey) {
        Long expireAt = hashExpiry.get(key);
        if (expireAt != null && System.currentTimeMillis() > expireAt) {
            hashStore.remove(key);
            hashExpiry.remove(key);
            return null;
        }
        ConcurrentHashMap<String, String> hash = hashStore.get(key);
        return hash != null ? hash.get(hashKey) : null;
    }

    /**
     * Atomically increment a hash value by delta.
     * Returns the new value after increment.
     */
    public long hincrBy(String key, String hashKey, long delta) {
        ConcurrentHashMap<String, String> hash = hashStore
                .computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        AtomicInteger result = new AtomicInteger();
        hash.merge(hashKey, String.valueOf(delta),
                (old, d) -> {
                    int newVal = Integer.parseInt(old) + (int) delta;
                    result.set(newVal);
                    return String.valueOf(newVal);
                });
        return result.get();
    }

    // === Set operations ===

    public void sadd(String key, String value) {
        setStore.computeIfAbsent(key, k -> new CopyOnWriteArraySet<>()).add(value);
        setExpiry.put(key, System.currentTimeMillis() + HASH_TTL_MS);
    }

    // === Internal ===

    private static class CacheEntry<T> {
        final T value;
        final long expireAt;

        CacheEntry(T value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
