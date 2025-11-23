package com.cache;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class LRUCache<K> implements Serializable {
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

    public List<K> getKeysInAccessOrder() {
        return orderMap.keySet().stream().collect(Collectors.toList());
    }

    public void restoreOrder(List<K> keys) {
        orderMap.clear();
        for (K key : keys) {
            orderMap.put(key, Boolean.TRUE);
        }
    }
}