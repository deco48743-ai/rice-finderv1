package dev.asianricefinder.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class ChunkCache {
    private final Map<ChunkKey, ChunkScanResult> scans = new HashMap<>();

    public ChunkScanResult get(ChunkKey key) { return scans.get(key); }
    public void put(ChunkScanResult result) { scans.put(result.key(), result); }
    public Collection<ChunkScanResult> values() { return scans.values(); }
    public void remove(ChunkKey key) { scans.remove(key); }
    public void removeIf(Predicate<ChunkKey> predicate) { scans.keySet().removeIf(predicate); }
    public void clear() { scans.clear(); }
}
