package dev.asianricefinder.data;

import java.util.List;

public record ChunkScanResult(ChunkKey key, List<GeodeHit> geodes, boolean scannable, long scannedAtTick) {
    public int underDeepslateCount() { return (int) geodes.stream().filter(GeodeHit::underDeepslate).count(); }
}
