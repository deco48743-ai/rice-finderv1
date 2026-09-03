package dev.asianricefinder.scan;

import dev.asianricefinder.data.ChunkCache;
import dev.asianricefinder.data.ChunkKey;
import dev.asianricefinder.data.ChunkScanResult;
import dev.asianricefinder.data.GeodeHit;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Cross-chunk cluster calculation over cached client-visible geode centers. */
public final class GeodeClusterDetector {
    public Map<ChunkKey, Integer> clusters(ChunkCache cache, int radius) {
        List<GeodeHit> all = new ArrayList<>();
        for (ChunkScanResult result : cache.values()) if (result.scannable()) all.addAll(result.geodes());
        Map<ChunkKey, Integer> output = new HashMap<>();
        long radiusSquared = (long) radius * radius;
        for (GeodeHit hit : all) {
            int nearby = 0;
            for (GeodeHit other : all) if (hit != other && horizontalDistanceSquared(hit.center(), other.center()) <= radiusSquared) nearby++;
            if (nearby > 0) output.merge(hit.owner(), 1, Integer::sum);
        }
        return output;
    }

    private long horizontalDistanceSquared(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX(), dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
