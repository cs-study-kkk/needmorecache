package com.cache;

import java.util.LinkedHashMap;

public class LRUCache<K> {
    private final int capacity;
    private final LinkedHashMap<K, Boolean> orderMap;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.orderMap = new LinkedHashMap<>(capacity,
                0.75f,
                true); // 75% 초과 시 재해싱
    }

    public void recordAccess(K key) {
        orderMap.put(key, Boolean.TRUE);
    }

    public K findEvictionTarget() {
        if (orderMap.size() <= capacity) return null;
        return orderMap.entrySet().iterator().next().getKey();
    }

    public void remove(K key) {
        orderMap.remove(key);
    }
}