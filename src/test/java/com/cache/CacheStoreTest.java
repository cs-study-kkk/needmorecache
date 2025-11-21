package com.cache;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CacheStoreTest {

    private CacheStore<String, String> store;

    @BeforeEach
    void setUp() {
        store = new CacheStore<>(4);
    }

    @Test
    @DisplayName("저장한 값이 정상 조회되는지 확인")
    void testSetAndGet() {
        store.set("name", "John");

        String value = store.get("name");

        assertNotNull(value);
        assertEquals("John", value);
    }

    @Test
    @DisplayName("키 중복 저장 시 값이 정상적으로 덮어써지는지 확인")
    void testOverwrite() {
        store.set("key", "V1");
        store.set("key", "V2");

        assertEquals("V2", store.get("key"));
    }

    @Test
    @DisplayName("TTL 지정 시 만료된 키는 null 을 반환하는지 확인")
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
    @DisplayName("DEL 명령으로 키 삭제 후 존재 여부 검사")
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
    @DisplayName("존재하지 않는 키의 TTL 요청 시 -1 반환 확인")
    void testTTLForMissingKey() {
        assertEquals(-1, store.ttl("missing"));
    }

    @Test
    @DisplayName("LRU: 용량 초과 시 가장 오래된 키 제거")
    void testLRUEviction() {
        store.set("k1", "v1");
        store.set("k2", "v2");
        store.set("k3", "v3");
        store.set("k4", "v4");

        // 가장 오래된 항목(k1)에 접근해 순서를 갱신. (New LRU: k2)
        store.get("k1");

        // 용량 초과하는 5번째 키를 저장
        store.set("k5", "v5");

        assertFalse(store.exists("k2"), "가장 오래된 키 Eviction.");

        assertNotNull(store.get("k1"));
        assertNotNull(store.get("k3"));
        assertNotNull(store.get("k4"));
        assertNotNull(store.get("k5"));
    }

}