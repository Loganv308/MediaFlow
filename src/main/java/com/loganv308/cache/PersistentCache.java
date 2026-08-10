package com.loganv308.cache;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PersistentCache {
    private static final String CACHE_FILE = "media_cache.ser";
    
    private Map<String, FileRecord> cache;

    public PersistentCache() {
        if (Files.exists(Paths.get(CACHE_FILE))) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(CACHE_FILE))) {
                @SuppressWarnings("unchecked")
                ConcurrentHashMap<String, FileRecord> cache = new ConcurrentHashMap<>((Map<String, FileRecord>) in.readObject());
                System.out.println("Loaded " + cache.size() + " cached entries");
            } catch (Exception e) {
                System.out.println("Cache unreadable, starting fresh");
                cache = new ConcurrentHashMap<>();
            }
        } else {
            cache = new ConcurrentHashMap<>();
        }
    }

    public void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(CACHE_FILE))) {
            out.writeObject(cache);
            System.out.println("Cache saved: " + cache.size() + " entries");
        } catch (IOException e) {
            System.err.println("Failed to save cache: " + e.getMessage());
        }
    }

    public Map<String, FileRecord> getCache() { return cache; }
}
