package dev.asianricefinder.scan;

import dev.asianricefinder.data.ChunkKey;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Samples visible player movement; it has no packet or server-side inputs. */
public final class PlayerActivityTracker {
    private final Map<UUID, Sample> samples = new HashMap<>();

    public void sample(Level level, long tick) {
        samples.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
        for (Player player : level.players()) {
            if (player == Minecraft.getInstance().player) continue;
            Vec3 position = player.position();
            Sample old = samples.get(player.getUUID());
            boolean moving = old != null && old.position.distanceToSqr(position) > 1.0 && tick - old.tick <= 40;
            samples.put(player.getUUID(), new Sample(position, tick, moving));
        }
    }

    public Activity activityNear(Level level, ChunkKey key, int radius) {
        double x = key.minBlockX() + 8.0, z = key.minBlockZ() + 8.0;
        int players = 0, moving = 0;
        double radiusSquared = (double) radius * radius;
        for (Player player : level.players()) {
            if (player == Minecraft.getInstance().player) continue;
            double dx = player.getX() - x, dz = player.getZ() - z;
            if (dx * dx + dz * dz > radiusSquared) continue;
            players++;
            Sample sample = samples.get(player.getUUID());
            if (sample != null && sample.moving) moving++;
        }
        return new Activity(players, players * 3 + moving * 3);
    }

    public void clear() { samples.clear(); }
    public record Activity(int players, int score) { }
    private record Sample(Vec3 position, long tick, boolean moving) { }
}
