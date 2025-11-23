package com.cache.dto;

import com.cache.CacheEntry;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class SnapshotData<K, V> implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Map<K, CacheEntry<V>> storeData;
    public final List<K> lruKeys;

    public SnapshotData(Map<K, CacheEntry<V>> storeData, List<K> lruKeys) {
        this.storeData = storeData;
        this.lruKeys = lruKeys;
    }
}
