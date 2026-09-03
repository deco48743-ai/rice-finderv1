package dev.asianricefinder.scan;

import dev.asianricefinder.data.ChunkKey;
import dev.asianricefinder.data.GeodeHit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Finds connected, client-visible amethyst evidence. It never requests chunk data. */
public final class GeodeDetector {
    private static final int MAX_COMPONENT_SIZE = 4096;

    public List<GeodeHit> detect(Level level, LevelChunk chunk, ChunkKey target, int minY, int maxY) {
        List<GeodeHit> hits = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = target.minBlockX(); x < target.minBlockX() + 16; x++) for (int z = target.minBlockZ(); z < target.minBlockZ() + 16; z++) {
            for (int y = minY; y <= maxY; y++) {
                cursor.set(x, y, z);
                BlockPos start = cursor.immutable();
                if (visited.contains(start) || !isAmethystEvidence(chunk.getBlockState(cursor).getBlock())) continue;
                GeodeHit hit = flood(level, start, visited);
                // A component can cross a boundary. Only the chunk containing its canonical minimum owns it.
                if (hit != null && hit.owner().equals(target)) hits.add(hit);
            }
        }
        return hits;
    }

    private GeodeHit flood(Level level, BlockPos start, Set<BlockPos> visited) {
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        pending.add(start);
        visited.add(start);
        int count = 0, xTotal = 0, yTotal = 0, zTotal = 0;
        BlockPos canonical = start;

        while (!pending.isEmpty() && count < MAX_COMPONENT_SIZE) {
            BlockPos pos = pending.removeFirst();
            if (!isLoaded(level, pos) || !isAmethystEvidence(level.getBlockState(pos).getBlock())) continue;
            count++; xTotal += pos.getX(); yTotal += pos.getY(); zTotal += pos.getZ();
            if (compare(pos, canonical) < 0) canonical = pos;
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                BlockPos next = pos.offset(dx, dy, dz);
                if (visited.add(next) && isLoaded(level, next) && isAmethystEvidence(level.getBlockState(next).getBlock())) pending.add(next);
            }
        }
        if (count < 4) return null;
        BlockPos center = new BlockPos(xTotal / count, yTotal / count, zTotal / count);
        return new GeodeHit(center, ChunkKey.from(canonical), count, enclosedByDeepslate(level, center));
    }

    private boolean isLoaded(Level level, BlockPos pos) { return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null; }
    private boolean isAmethystEvidence(Block block) {
        return block == Blocks.AMETHYST_BLOCK || block == Blocks.BUDDING_AMETHYST || block == Blocks.AMETHYST_CLUSTER
            || block == Blocks.LARGE_AMETHYST_BUD || block == Blocks.MEDIUM_AMETHYST_BUD || block == Blocks.SMALL_AMETHYST_BUD;
    }
    private boolean enclosedByDeepslate(Level level, BlockPos center) {
        int deepslate = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dy == 0 && dz == 0) continue;
            Block block = level.getBlockState(center.offset(dx, dy, dz)).getBlock();
            if (block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE || block == Blocks.DEEPSLATE_TILES || block == Blocks.DEEPSLATE_BRICKS) deepslate++;
        }
        return deepslate >= 18;
    }
    private int compare(BlockPos a, BlockPos b) {
        int x = Integer.compare(a.getX(), b.getX());
        if (x != 0) return x;
        int z = Integer.compare(a.getZ(), b.getZ());
        return z != 0 ? z : Integer.compare(a.getY(), b.getY());
    }
}
