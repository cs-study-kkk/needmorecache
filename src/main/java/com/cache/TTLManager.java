package com.cache;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class TTLManager {

    private final CacheStore<String, ?> store;
    private final ScheduledExecutorService scheduler;
    private final long period;

    public TTLManager(CacheStore<String, ?> store, long periodMillis) {
        this.store = store;
        this.period = periodMillis;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::cleanExpired,
                period, period, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private void cleanExpired() {
        try {
            for (var entry : store.entries().entrySet()) {
                String key = entry.getKey();
                CacheEntry<?> cacheEntry = entry.getValue();

                if (cacheEntry.isExpired()) {
                    store.del(key);
                    System.out.println("[TTL] expired: " + key);
                }
            }
        } catch (Exception ex) {
            System.err.println("[TTL] Error: " + ex.getMessage());
        }
    }
}
