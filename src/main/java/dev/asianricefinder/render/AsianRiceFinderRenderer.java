package dev.asianricefinder.render;

import dev.asianricefinder.data.ChunkKey;
import dev.asianricefinder.data.DetectionScore;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;

import java.util.Map;

/** Rendering has no scanning side effects. */
public final class AsianRiceFinderRenderer {
    public void render(Render3DEvent event, Map<ChunkKey, DetectionScore> qualified, int renderDistance, int height, SettingColor side, SettingColor line) {
        if (Minecraft.getInstance().player == null) return;
        double maxDistanceSquared = (double) renderDistance * renderDistance;
        for (Map.Entry<ChunkKey, DetectionScore> entry : qualified.entrySet()) {
            ChunkKey key = entry.getKey();
            double dx = Minecraft.getInstance().player.getX() - (key.minBlockX() + 8.0);
            double dz = Minecraft.getInstance().player.getZ() - (key.minBlockZ() + 8.0);
            if (dx * dx + dz * dz > maxDistanceSquared) continue;
            double middleY = Minecraft.getInstance().player.getY();
            AABB box = new AABB(key.minBlockX(), middleY - height / 2.0, key.minBlockZ(), key.minBlockX() + 16, middleY + height / 2.0, key.minBlockZ() + 16);
            event.renderer.box(box, side, line, ShapeMode.Both, 0);
        }
    }
}
