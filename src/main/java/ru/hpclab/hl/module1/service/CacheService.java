package ru.hpclab.hl.module1.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class CacheService<T> {
    public final Map<Long, T> cache = new HashMap<>();
    private final String typeName;

    public CacheService() {
        this.typeName = "Unknown";
    }

    public CacheService(String typeName) {
        this.typeName = typeName;
    }

    /**
     * Get an item from the cache by ID.
     *
     * @param id The item ID
     * @return The cached item, or null if not found
     */
    public T get(Long id) {
        T item = cache.get(id);
        if (item != null) {
            log.info("Cache hit for {} ID: {}", typeName, id);
        } else {
            log.info("Cache miss for {} ID: {}", typeName, id);
        }
        return item;
    }

    /**
     * Add an item to the cache if it doesn't already exist.
     *
     * @param id   The item ID
     * @param item The item to cache
     * @return true if the item was added, false if it already existed
     */
    public boolean add(Long id, T item) {
        if (id == null || item == null) {
            log.warn("Attempted to add invalid {}: id={}, item={}", typeName, id, item);
            return false;
        }
        if (cache.containsKey(id)) {
            log.info("Skipped adding {} ID: {}, already exists", typeName, id);
            return false;
        }
        cache.put(id, item);
        log.info("Added {} ID: {}", typeName, id);
        return true;
    }

    /**
     * Add or update an item in the cache.
     *
     * @param id   The item ID
     * @param item The item to cache
     */
    public void set(Long id, T item) {
        if (id != null && item != null) {
            cache.put(id, item);
            log.info("Cached {} ID: {}", typeName, id);
        } else {
            log.warn("Attempted to cache invalid {}: id={}, item={}", typeName, id, item);
        }
    }

    /**
     * Clear all items from the cache.
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        log.info("Cleared {} cache. Removed {} entries", typeName, size);
    }

    /**
     * Check if the cache is empty.
     *
     * @return true if the cache is empty, false otherwise
     */
    public boolean isEmpty() {
        boolean empty = cache.isEmpty();
        log.info("Checked if {} cache is empty: {}", typeName, empty);
        return empty;
    }

    /**
     * Log cache statistics periodically.
     */
    @Scheduled(fixedRateString = "${cache.log.interval:4000}")
    public void logCacheStatistics() {
        log.info("{} cache size: {}", typeName, cache.size());
    }
}