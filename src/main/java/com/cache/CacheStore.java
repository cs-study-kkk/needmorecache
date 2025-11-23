package com.cache;

import com.cache.dto.SnapshotData;

import java.io.Serializable;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class CacheStore<K, V> implements Serializable {

    private final Map<K, CacheEntry<V>> store = new ConcurrentHashMap<>();
    private final LRUCache<K> lru;
    private final transient ReentrantLock lruLock = new ReentrantLock();

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

    public void restoreFromSnapshot(Map<K, CacheEntry<V>> restoredStore, List<K> restoredLruKeys) {
        this.store.clear();
        this.store.putAll(restoredStore);

        this.lru.restoreOrder(restoredLruKeys);

        System.out.println("[Snapshot] CacheStore restored successfully. Keys: " + this.store.size());
    }

    public SnapshotData<K, V> getSnapshotData() {
        lruLock.lock();
        try {
            Map<K, CacheEntry<V>> data = Map.copyOf(store);
            List<K> keys = lru.getKeysInAccessOrder();
            return new SnapshotData<>(data, keys);
        } finally {
            lruLock.unlock();
        }
    }

    private boolean checkExpired(K key, CacheEntry<V> entry) {
        if (entry == null || entry.isExpired()) {
            return true;
        }
        return false;
    }

    public Map<K, CacheEntry<V>> dumpAll() {
        return Collections.unmodifiableMap(new HashMap<>(store));
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
