package com.cache;

import java.io.Closeable;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manages TTL (time-to-live) metadata for cache entries.
 * <p>
 * The manager keeps track of the expiration timestamp for each key and
 * schedules an asynchronous task that triggers when a key needs to be
 * expired. The actual removal from the cache is delegated to the
 * {@code onExpire} callback.
 */
public class TTLManager implements Closeable {

    private final ScheduledExecutorService scheduler;
    private final ConcurrentMap<String, ScheduledFuture<?>> ttlTasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> expiryTimes = new ConcurrentHashMap<>();
    private final Consumer<String> onExpire;
    private final Clock clock;

    /**
     * Creates a {@link TTLManager} with the provided callback.
     *
     * @param onExpire callback invoked whenever a key reaches its TTL. The
     *                 callback is responsible for removing the key from the
     *                 actual cache store. Must not be {@code null}.
     */
    public TTLManager(Consumer<String> onExpire) {
        this(onExpire, Clock.systemUTC());
    }

    TTLManager(Consumer<String> onExpire, Clock clock) {
        this.onExpire = Objects.requireNonNull(onExpire, "onExpire");
        this.clock = Objects.requireNonNull(clock, "clock");
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "ttl-manager");
            thread.setDaemon(true);
            return thread;
        };
        this.scheduler = Executors.newSingleThreadScheduledExecutor(factory);
    }

    /**
     * Registers a TTL for the given key. If a TTL already exists, it is
     * replaced with the new value.
     *
     * @param key        cache key
     * @param ttlSeconds ttl duration in seconds. A value less than or equal to
     *                   zero immediately expires the key.
     */
    public void scheduleTTL(String key, long ttlSeconds) {
        Objects.requireNonNull(key, "key");
        cancelTTL(key);

        if (ttlSeconds <= 0) {
            triggerExpiration(key);
            return;
        }

        long expireAtMillis = nowMillis() + TimeUnit.SECONDS.toMillis(ttlSeconds);
        expiryTimes.put(key, expireAtMillis);

        ScheduledFuture<?> future = scheduler.schedule(() -> handleExpiration(key, expireAtMillis),
                ttlSeconds,
                TimeUnit.SECONDS);
        ttlTasks.put(key, future);
    }

    /**
     * Removes any TTL associated with the key. The entry will no longer be
     * automatically expired.
     */
    public void cancelTTL(String key) {
        if (key == null) {
            return;
        }
        ScheduledFuture<?> future = ttlTasks.remove(key);
        if (future != null) {
            future.cancel(false);
        }
        expiryTimes.remove(key);
    }

    /**
     * Returns the remaining TTL in seconds for the given key.
     *
     * @param key cache key
     * @return remaining TTL in seconds, {@code -1} if no TTL is associated
     *         with the key.
     */
    public long getRemainingTTL(String key) {
        Objects.requireNonNull(key, "key");
        Long expiresAt = expiryTimes.get(key);
        if (expiresAt == null) {
            return -1L;
        }
        long diff = expiresAt - nowMillis();
        return diff <= 0 ? 0 : TimeUnit.MILLISECONDS.toSeconds(diff);
    }

    /**
     * Invoked during shutdown to cancel all scheduled tasks and stop the
     * scheduler.
     */
    @Override
    public void close() {
        ttlTasks.values().forEach(future -> future.cancel(false));
        ttlTasks.clear();
        expiryTimes.clear();
        scheduler.shutdownNow();
    }

    private void handleExpiration(String key, long expectedExpireAt) {
        Long currentExpireAt = expiryTimes.get(key);
        if (currentExpireAt == null || currentExpireAt != expectedExpireAt) {
            // TTL has been updated or removed, ignore this task.
            return;
        }
        ttlTasks.remove(key);
        expiryTimes.remove(key);
        triggerExpiration(key);
    }

    private void triggerExpiration(String key) {
        try {
            onExpire.accept(key);
        } catch (RuntimeException e) {
            // The cache implementation should decide how to handle failures.
            // Swallowing the exception keeps the TTL thread alive.
        }
    }

    private long nowMillis() {
        return clock.millis();
    }
}
