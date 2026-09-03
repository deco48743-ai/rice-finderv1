package dev.asianricefinder.scan;

import dev.asianricefinder.data.ChunkKey;
import dev.asianricefinder.data.ChunkScanResult;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/** Performs one bounded scan at a time on Minecraft's client thread. */
public final class ChunkScanner {
    private final GeodeDetector geodes = new GeodeDetector();

    public ChunkScanResult scan(Level level, ChunkKey key, int scanDepth, long tick) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(key.x(), key.z());
        if (chunk == null) return new ChunkScanResult(key, java.util.List.of(), false, tick);
        if (Minecraft.getInstance().player == null) return new ChunkScanResult(key, java.util.List.of(), false, tick);
        int playerY = (int) Math.floor(Minecraft.getInstance().player.getY());
        int minY = Math.max(level.getMinY(), playerY - scanDepth);
        int maxY = Math.min(level.getMaxY() - 1, playerY + scanDepth);
        return new ChunkScanResult(key, geodes.detect(level, chunk, key, minY, maxY), true, tick);
    }
}
