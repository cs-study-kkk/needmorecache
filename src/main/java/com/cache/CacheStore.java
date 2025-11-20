package com.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheStore<K, V> {
    private final Map<K, CacheEntry<V>> store = new ConcurrentHashMap<>();

    // no TTL
    public void set(K key, V value) {
        set(key, value, -1);
    }

    public void set(K key, V value, long ttl) {
        store.put(key, new CacheEntry<>(value, ttl));
    }

    public V get(K key) {
        CacheEntry<V> entry = store.get(key);

        if (entry == null || checkAndEvictIfExpired(key, entry))
            return null;

        return entry.getValue();
    }

    public boolean del(K key) {
        return store.remove(key) != null;
    }

    public boolean exists(K key) {
        return get(key) != null;
    }

    public long ttl(K key) {
        CacheEntry<V> entry = store.get(key);

        if (entry == null || checkAndEvictIfExpired(key, entry))
            return -1;

        return entry.getRemainingTTL();
    }

    public void clear() {
        store.clear();
    }

    private boolean checkAndEvictIfExpired(K key, CacheEntry<V> entry){
        if (entry != null && entry.isExpired()) {
            store.remove(key);
            return true;
        }
        return false;
    }

    public Map<K, CacheEntry<V>> dumpAll() {
        return Collections.unmodifiableMap(new HashMap<>(store));
    }
}
