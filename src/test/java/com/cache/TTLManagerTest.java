package com.cache;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TTLManagerTest {

    private TTLManager ttlManager;
    private CacheStore<String, String> cacheStore;

    @BeforeEach
    void setUp() {
        cacheStore = new CacheStore<>(3);
    }

    @AfterEach
    void tearDown() {
        if (ttlManager != null) {
            ttlManager.stop();
        }
    }

    @Test
    @DisplayName("수동 만료 테스트")
    void testTTLExpiration() throws InterruptedException {
        cacheStore.set("hello", "world", 500);

        assertEquals("world", cacheStore.get("hello"));

        Thread.sleep(600);

        assertNull(cacheStore.get("hello"));
    }

    @Test
    @DisplayName("능동 만료 테스트")
    void testTTLManagerAutoDelete() throws InterruptedException {
        // 주기 200ms
        ttlManager = new TTLManager(cacheStore, 200);
        ttlManager.start();

        cacheStore.set("key", "value", 300);

        assertEquals(cacheStore.get("key"),"value");

        Thread.sleep(800);

        assertNull(cacheStore.get("key"));
    }

    @Test
    @DisplayName("TTL 남은 시간 감소 테스트")
    void testTTLRemainingTimeDecreases() throws InterruptedException {
        cacheStore.set("key", "value", 1000);

        long ttlBefore = cacheStore.ttl("key");
        assertTrue(ttlBefore > 0);

        Thread.sleep(300);

        long ttlAfter = cacheStore.ttl("key");
        assertTrue(ttlAfter > 0);

        // 대기 시간 만큼 ttl 감소되었는지 확인
        assertTrue(ttlAfter < ttlBefore);
    }
}
