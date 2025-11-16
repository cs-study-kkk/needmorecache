package com.cache;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CacheStoreTest {

    private CacheStore<String, String> store= new CacheStore<>();


    @Test
    void testSetAndGet() {
        store.set("name", "John");

        String value = store.get("name");

        assertNotNull(value);
        assertEquals("John", value);
    }

    @Test
    void testOverwrite() {
        store.set("key", "V1");
        store.set("key", "V2");

        assertEquals("V2", store.get("key"));
    }

    @Test
    void testExpiration() throws InterruptedException {
        store.set("temp", "data", 100);

        // 즉시 조회
        assertEquals("data", store.get("temp"));

        // 150ms 대기
        Thread.sleep(150);

        // 다시 조회 -> null
        assertNull(store.get("temp"));
    }

    @Test
    void testDelete() {
        store.set("key", "V1");
        assertTrue(store.exists("key"));

        store.del("key");

        assertFalse(store.exists("key"));
        assertNull(store.get("key"));
    }

    @Test
    void testTTLNotExpiredYet() throws InterruptedException {
        store.set("key", "val", 3000);
        Thread.sleep(1000);

        assertNotNull(store.get("key"));
        assertEquals("val", store.get("key"));
    }

    @Test
    void testTTLCheck() {
        store.set("key", "val", 5000);

        assertTrue(store.ttl("key") > 0);
    }

    @Test
    void testTTLForMissingKey() {
        assertEquals(-1, store.ttl("missing"));
    }

}