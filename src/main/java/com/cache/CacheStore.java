package com.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class CacheStore<K, V> {

    private final Map<K, CacheEntry<V>> store = new ConcurrentHashMap<>();
    private final LRUCache<K> lru;
    private final ReentrantLock lruLock = new ReentrantLock();

    public CacheStore(int capacity) {
        this.lru = new LRUCache<>(capacity);
    }

    // no TTL
    public void set(K key, V value) {
        set(key, value, -1);
    }

    public void set(K key, V value, long ttl) {
        store.put(key, new CacheEntry<>(value, ttl));

        updateLRU(key);
    }

    public V get(K key) {
        CacheEntry<V> entry = store.get(key);

        if (checkExpired(key, entry)) {
            return null;
        }

        updateLRU(key);
        return entry.getValue();
    }

    public boolean del(K key) {
        store.remove(key);

        lruLock.lock();
        try {
            lru.remove(key);
        } finally {
            lruLock.unlock();
        }
        return true;
    }

    public boolean exists(K key) {
        return get(key) != null;
    }

    public long ttl(K key) {
        CacheEntry<V> entry = store.get(key);

        if (checkExpired(key, entry)) {
            return -1;
        }

        return entry.getRemainingTTL();
    }

    public Map<K, CacheEntry<V>> entries() {
        return Map.copyOf(store);
    }

    private boolean checkExpired(K key, CacheEntry<V> entry) {
        if (entry == null || entry.isExpired()) {
            return true;
        }
        return false;
    }

    //LRU 처리
    private void updateLRU(K key) {
        lruLock.lock();
        try {
            lru.recordAccess(key);
            K evict = lru.findEvictionTarget();

            if (evict != null) {
                del(evict);
                System.out.println("[LRU] evicted key: " + evict);
            }
        } finally {
            lruLock.unlock();
        }
    }
}
