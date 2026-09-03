package dev.asianricefinder.data;

import net.minecraft.core.BlockPos;

public record ChunkKey(int x, int z) {
    public static ChunkKey from(BlockPos pos) { return new ChunkKey(pos.getX() >> 4, pos.getZ() >> 4); }
    public int minBlockX() { return x << 4; }
    public int minBlockZ() { return z << 4; }
}
