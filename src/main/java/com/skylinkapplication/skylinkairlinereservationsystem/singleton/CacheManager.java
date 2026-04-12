package com.skylinkapplication.skylinkairlinereservationsystem.singleton;

import java.util.*;

public class CacheManager {
    private static CacheManager instance;
    private final Map<String, CacheEntry> cache = new HashMap<>();
    private final long DEFAULT_TTL = 3600000; // 1 hour in milliseconds

    private CacheManager() {
    }

    public static synchronized CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }

    public synchronized void put(String key, Object value) {
        cache.put(key, new CacheEntry(value, System.currentTimeMillis() + DEFAULT_TTL));
    }

    public synchronized void put(String key, Object value, long ttl) {
        cache.put(key, new CacheEntry(value, System.currentTimeMillis() + ttl));
    }

    public synchronized Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.getExpiryTime()) {
            cache.remove(key);
            return null;
        }
        return entry.getValue();
    }

    public synchronized void remove(String key) {
        cache.remove(key);
    }

    public synchronized void clearExpired() {
        long currentTime = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> currentTime > entry.getValue().getExpiryTime());
    }

    public synchronized void clear() {
        cache.clear();
    }

    public synchronized int getCacheSize() {
        return cache.size();
    }

    private static class CacheEntry {
        private final Object value;
        private final long expiryTime;

        CacheEntry(Object value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }

        Object getValue() { return value; }
        long getExpiryTime() { return expiryTime; }
    }
}