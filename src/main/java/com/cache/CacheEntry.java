package com.cache;

import java.io.Serializable;

public class CacheEntry<T> implements Serializable {
    private final T value;
    private final long expireAt; // ex) currentTimeMillis + 1000ms

    public CacheEntry(T value, long ttl) {
        this.value = value;
        this.expireAt = calculateExpireAt(ttl);
    }

    public boolean isExpired() {
        return expireAt != -1 && System.currentTimeMillis() >= expireAt;
    }

    public T getValue() {
        return value;
    }

    public long getRemainingTTL() {
        if (expireAt == -1) return -1;
        return Math.max(0, expireAt - System.currentTimeMillis());
    }

    private static long calculateExpireAt(long ttl) {
        return ttl > 0 ? System.currentTimeMillis() + ttl : -1;
    }

    @Override
    public String toString() {
        return "CacheEntry{" +
                "value=" + value +
                ", expireAt=" + expireAt +
                '}';
    }
}
