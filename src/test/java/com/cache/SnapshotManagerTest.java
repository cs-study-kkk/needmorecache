package com.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotManagerTest {

    private static final int CAPACITY = 3;
    private static final long TTL_PERIOD_MS = 200;
    private static final String DEFAULT_SNAPSHOT_FILE = "cache_snapshot.dat";
    private static final String CUSTOM_SNAPSHOT_FILE = "custom_test_snapshot.dat";

    private TTLManager ttlManager;

    @BeforeEach
    @AfterEach
    void cleanUp() throws IOException {
        if (ttlManager != null) {
            ttlManager.stop();
        }

        // 스냅샷 파일 정리
        Files.deleteIfExists(Paths.get(DEFAULT_SNAPSHOT_FILE));
        Files.deleteIfExists(Paths.get(CUSTOM_SNAPSHOT_FILE));

        // .corrupted 파일도 정리
        try (Stream<Path> stream = Files.list(Paths.get("."))) {
            List<Path> corruptedFiles = stream
                    .filter(p -> {
                        String fileName = p.getFileName().toString();
                        return fileName.startsWith("cache_snapshot.dat.corrupted")
                                || fileName.startsWith("custom_test_snapshot.dat.corrupted");
                    })
                    .collect(Collectors.toList());

            for (Path path : corruptedFiles) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    @DisplayName("스냅샷 복원 후 LRU 순서가 정상적으로 동작하는지 테스트")
    void testLruOrderRestorationAfterSnapshot() {

        // 기본 데이터 넣고 LRU 동작 후 저장
        CacheStore<String, String> originalStore = new CacheStore<>(CAPACITY);
        SnapshotManager<String, String> originalManager = new SnapshotManager<>(originalStore);

        originalStore.set("A", "Value A");
        originalStore.set("B", "Value B");
        originalStore.set("C", "Value C");

        originalStore.get("A");
        originalStore.get("B");

        originalManager.saveSnapshot();
        assertTrue(new File(DEFAULT_SNAPSHOT_FILE).exists());

        // 스냅샷 복원
        CacheStore<String, String> restoredStore = new CacheStore<>(CAPACITY);
        SnapshotManager<String, String> restoredManager = new SnapshotManager<>(restoredStore);
        restoredManager.loadSnapshot();

        // 새 요소 추가되면 C가 제거되어야 함
        assertEquals(3, restoredStore.entries().size());
        restoredStore.set("D", "Value D");

        assertFalse(restoredStore.entries().containsKey("C"));
        assertTrue(restoredStore.entries().containsKey("D"));
        assertEquals(3, restoredStore.entries().size());
    }

    @Test
    @DisplayName("TTL 값이 스냅샷 복원 후에도 유지되는지 테스트")
    void testTTLRestorationAndExpiration() throws InterruptedException {

        CacheStore<String, String> originalStore = new CacheStore<>(CAPACITY);
        SnapshotManager<String, String> originalManager = new SnapshotManager<>(originalStore);

        ttlManager = new TTLManager(originalStore, TTL_PERIOD_MS);
        ttlManager.start();

        originalStore.set("TTL_KEY", "Expire soon", 2000L);

        TimeUnit.MILLISECONDS.sleep(500);
        ttlManager.stop();
        originalManager.saveSnapshot();

        CacheStore<String, String> restoredStore = new CacheStore<>(CAPACITY);
        SnapshotManager<String, String> restoredManager = new SnapshotManager<>(restoredStore);
        restoredManager.loadSnapshot();

        ttlManager = new TTLManager(restoredStore, TTL_PERIOD_MS);
        ttlManager.start();

        assertTrue(restoredStore.exists("TTL_KEY"));
        long remainingTTL = restoredStore.ttl("TTL_KEY");
        assertTrue(remainingTTL > 500);

        TimeUnit.MILLISECONDS.sleep(2500);
        assertFalse(restoredStore.exists("TTL_KEY"));
    }

    @Test
    @DisplayName("커스텀 파일 경로 사용 시 스냅샷 저장/복원이 제대로 되는지 테스트")
    void testCustomSnapshotFilePath() {

        CacheStore<String, String> store = new CacheStore<>(CAPACITY);
        SnapshotManager<String, String> manager = new SnapshotManager<>(store, CUSTOM_SNAPSHOT_FILE);

        store.set("key", "value");

        manager.saveSnapshot();

        assertTrue(new File(CUSTOM_SNAPSHOT_FILE).exists());
        assertFalse(new File(DEFAULT_SNAPSHOT_FILE).exists());

        CacheStore<String, String> newStore = new CacheStore<>(CAPACITY);
        SnapshotManager<String, String> newManager = new SnapshotManager<>(newStore, CUSTOM_SNAPSHOT_FILE);
        newManager.loadSnapshot();

        assertEquals("value", newStore.get("key"));
    }

    @Test
    @DisplayName("손상된 스냅샷 파일을 읽었을 때 정상적으로 복구 처리되는지 테스트")
    void testLoadSnapshotWithCorruptedFile() throws IOException {

        // 일부러 손상된 스냅샷 파일 생성
        Files.writeString(Paths.get(DEFAULT_SNAPSHOT_FILE), "This is corrupted data");

        CacheStore<String, String> store = new CacheStore<>(CAPACITY);
        SnapshotManager<String, String> manager = new SnapshotManager<>(store);

        // 손상된 파일 로드 -> 내부에서 백업 및 초기화
        manager.loadSnapshot();

        assertTrue(store.entries().isEmpty());
        assertFalse(new File(DEFAULT_SNAPSHOT_FILE).exists());

        long corruptedFileCount;
        try (Stream<Path> stream = Files.list(Paths.get("."))) {
            corruptedFileCount = stream
                    .filter(p -> p.getFileName().toString().startsWith("cache_snapshot.dat.corrupted"))
                    .count();
        }
        assertEquals(1, corruptedFileCount);
    }
}
