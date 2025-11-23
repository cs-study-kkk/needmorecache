package com.cache;

import com.cache.dto.SnapshotData;

import java.io.*;

public class SnapshotManager<K, V> {
    private final CacheStore<K, V> store;
    private final String snapshotFilePath;

    public SnapshotManager(CacheStore<K, V> store) {
        this(store, "cache_snapshot.dat");
    }

    public SnapshotManager(CacheStore<K, V> store, String snapshotFilePath) {
        this.store = store;
        this.snapshotFilePath = snapshotFilePath;
    }

    public void saveSnapshot() {
        try {
            SnapshotData<K, V> snapshot = store.getSnapshotData();

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(snapshotFilePath))) {
                oos.writeObject(snapshot);
                System.out.println("[snapshot] saved (" + snapshot.storeData.size() + " keys)");
            }

        } catch (IOException e) {
            System.err.println("[snapshot] save error: " + e.getMessage());
        }
    }

    public void loadSnapshot() {
        File file = new File(snapshotFilePath);
        if (!file.exists()) {
            System.out.println("[snapshot] no file, empty cache");
            return;
        }

        try (FileInputStream fis = new FileInputStream(snapshotFilePath);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            @SuppressWarnings("unchecked")
            SnapshotData<K, V> snapshot = (SnapshotData<K, V>) ois.readObject();

            store.restoreFromSnapshot(snapshot.storeData, snapshot.lruKeys);
            System.out.println("[snapshot] loaded");

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[snapshot] load error: " + e.getMessage());

            File corruptedFile = new File(snapshotFilePath);
            if (corruptedFile.exists()) {
                String renamed = snapshotFilePath + ".corrupted." + System.currentTimeMillis();
                if (corruptedFile.renameTo(new File(renamed))) {
                    System.err.println("[snapshot] renamed corrupted file -> " + renamed);
                } else {
                    System.err.println("[snapshot] failed to rename corrupted file");
                }
            }
        }
    }
}
