package dev.asianricefinder.data;

import net.minecraft.core.BlockPos;

public record GeodeHit(BlockPos center, ChunkKey owner, int evidenceBlocks, boolean underDeepslate) { }
