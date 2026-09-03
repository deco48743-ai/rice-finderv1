package dev.asianricefinder.modules;

import dev.asianricefinder.AsianRiceFinderAddon;
import dev.asianricefinder.data.ChunkCache;
import dev.asianricefinder.data.ChunkKey;
import dev.asianricefinder.data.ChunkScanResult;
import dev.asianricefinder.data.DetectionScore;
import dev.asianricefinder.render.AsianRiceFinderRenderer;
import dev.asianricefinder.scan.ChunkScanner;
import dev.asianricefinder.scan.GeodeClusterDetector;
import dev.asianricefinder.scan.PlayerActivityTracker;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AsianRiceFinderModule extends Module {
    private final SettingGroup general = settings.getDefaultGroup();
    private final SettingGroup performance = settings.createGroup("Performance");
    private final SettingGroup render = settings.createGroup("Render");
    private final SettingGroup display = settings.createGroup("Information");

    private final Setting<Integer> minimumGeodes = general.add(new IntSetting.Builder().name("minimum-geodes").description("Geodes needed in a chunk before it can qualify.").defaultValue(1).min(1).sliderMax(10).build());
    private final Setting<Integer> clusterRadius = general.add(new IntSetting.Builder().name("cluster-radius").description("Maximum horizontal distance between geodes in one cluster.").defaultValue(50).range(4, 128).sliderMax(128).build());
    private final Setting<Integer> minimumPlayerActivity = general.add(new IntSetting.Builder().name("minimum-player-activity").description("Minimum player activity score.").defaultValue(0).range(0, 100).sliderMax(50).build());
    private final Setting<Integer> minimumTotalScore = general.add(new IntSetting.Builder().name("minimum-total-score").description("Minimum combined score for a green highlight.").defaultValue(20).range(1, 200).sliderMax(100).build());
    private final Setting<Integer> confidenceThreshold = general.add(new IntSetting.Builder().name("confidence-threshold").description("Minimum confidence percentage for a highlight.").defaultValue(50).range(1, 100).sliderMax(100).build());
    private final Setting<Integer> scanRadius = performance.add(new IntSetting.Builder().name("scan-radius").description("Horizontal radius around the player, in blocks.").defaultValue(128).range(16, 512).sliderMax(256).build());
    private final Setting<Integer> scanDepth = performance.add(new IntSetting.Builder().name("scan-depth").description("Vertical client-visible range above and below the player.").defaultValue(96).range(16, 384).sliderMax(256).build());
    private final Setting<Integer> scanInterval = performance.add(new IntSetting.Builder().name("scan-interval").description("Ticks between queue refreshes.").defaultValue(20).range(5, 200).sliderMax(100).build());
    private final Setting<Integer> chunksPerUpdate = performance.add(new IntSetting.Builder().name("chunks-per-update").description("Maximum loaded chunks scanned at each update.").defaultValue(2).range(1, 16).sliderMax(8).build());
    private final Setting<Integer> renderDistance = render.add(new IntSetting.Builder().name("render-distance").description("Maximum distance to draw highlights, in blocks.").defaultValue(160).range(16, 512).sliderMax(256).build());
    private final Setting<Integer> highlightHeight = render.add(new IntSetting.Builder().name("highlight-height").description("Medium outline height, in blocks.").defaultValue(16).range(4, 64).sliderMax(32).build());
    private final Setting<SettingColor> sideColor = render.add(new ColorSetting.Builder().name("side-color").description("Green highlight fill and opacity.").defaultValue(new SettingColor(30, 220, 90, 30)).build());
    private final Setting<SettingColor> lineColor = render.add(new ColorSetting.Builder().name("line-color").description("Green highlight outline.").defaultValue(new SettingColor(30, 255, 100, 220)).build());
    private final Setting<Boolean> lookInfo = display.add(new BoolSetting.Builder().name("look-information").description("Show details when looking at a highlighted chunk.").defaultValue(true).build());
    private final Setting<Boolean> chatSummary = display.add(new BoolSetting.Builder().name("chat-summary").description("Announce changes in the number of high-confidence chunks.").defaultValue(true).build());

    private final ChunkCache cache = new ChunkCache();
    private final ChunkScanner scanner = new ChunkScanner();
    private final GeodeClusterDetector clusters = new GeodeClusterDetector();
    private final PlayerActivityTracker players = new PlayerActivityTracker();
    private final AsianRiceFinderRenderer renderer = new AsianRiceFinderRenderer();
    private final ArrayDeque<ChunkKey> queue = new ArrayDeque<>();
    private final Set<ChunkKey> queued = new HashSet<>();
    private final Map<ChunkKey, DetectionScore> qualified = new HashMap<>();
    private long ticks;
    private int lastAnnouncement = -1;

    public AsianRiceFinderModule() { super(AsianRiceFinderAddon.CATEGORY, "asian-rice-finder", "Scores loaded chunks for client-visible amethyst and player activity."); }

    @Override public void onActivate() { clear(); }
    @Override public void onDeactivate() { clear(); }

    @EventHandler private void onChunkData(ChunkDataEvent event) {
        queue(event.chunk().getPos().x(), event.chunk().getPos().z());
    }

    @EventHandler private void onTick(TickEvent.Post event) {
        ticks++;
        Level level = Minecraft.getInstance().level;
        if (level == null || Minecraft.getInstance().player == null) return;
        players.sample(level, ticks);
        if (ticks % scanInterval.get() == 0) refreshQueue(level);
        for (int i = 0; i < chunksPerUpdate.get() && !queue.isEmpty(); i++) scanNext(level);
        rebuildScores(level);
        announceIfChanged();
        if (lookInfo.get() && ticks % 10 == 0) showLookInfo();
    }

    @EventHandler private void onRender(Render3DEvent event) {
        renderer.render(event, qualified, renderDistance.get(), highlightHeight.get(), sideColor.get(), lineColor.get());
    }

    private void refreshQueue(Level level) {
        int centerX = Minecraft.getInstance().player.getBlockX() >> 4;
        int centerZ = Minecraft.getInstance().player.getBlockZ() >> 4;
        int chunkRadius = (scanRadius.get() + 15) >> 4;
        cache.removeIf(key -> Math.abs(key.x() - centerX) > chunkRadius || Math.abs(key.z() - centerZ) > chunkRadius || level.getChunkSource().getChunkNow(key.x(), key.z()) == null);
        for (int x = centerX - chunkRadius; x <= centerX + chunkRadius; x++) for (int z = centerZ - chunkRadius; z <= centerZ + chunkRadius; z++) {
            ChunkKey key = new ChunkKey(x, z);
            if (level.getChunkSource().getChunkNow(x, z) == null) { cache.remove(key); continue; }
            ChunkScanResult previous = cache.get(key);
            if (previous == null || ticks - previous.scannedAtTick() >= scanInterval.get() * 10L) queue(key);
        }
    }

    private void scanNext(Level level) {
        ChunkKey key = queue.removeFirst();
        queued.remove(key);
        ChunkScanResult result = scanner.scan(level, key, scanDepth.get(), ticks);
        if (result.scannable()) cache.put(result); else cache.remove(key);
    }

    private void rebuildScores(Level level) {
        qualified.clear();
        Map<ChunkKey, Integer> clusterCounts = clusters.clusters(cache, clusterRadius.get());
        for (ChunkScanResult result : cache.values()) {
            if (!result.scannable()) continue;
            PlayerActivityTracker.Activity activity = players.activityNear(level, result.key(), scanRadius.get());
            int clusterCount = clusterCounts.getOrDefault(result.key(), 0);
            int geodeScore = result.geodes().size() * 12;
            int clusterScore = clusterCount * 8;
            int total = geodeScore + clusterScore + activity.score();
            int confidence = Math.min(100, result.geodes().size() * 35 + clusterCount * 20 + (result.underDeepslateCount() > 0 ? 10 : 0));
            DetectionScore score = new DetectionScore(result.geodes().size(), clusterCount, activity.players(), activity.score(), total, confidence, result.underDeepslateCount() > 0);
            if (score.geodes() >= minimumGeodes.get() && score.playerActivity() >= minimumPlayerActivity.get() && score.total() >= minimumTotalScore.get() && score.confidence() >= confidenceThreshold.get()) qualified.put(result.key(), score);
        }
    }

    private void showLookInfo() {
        if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult hit)) return;
        ChunkKey key = ChunkKey.from(hit.getBlockPos());
        DetectionScore score = qualified.get(key);
        if (score == null) return;
        ChatUtils.sendMsg(260901, ChatFormatting.GREEN, "Asian Rice Finder | Geodes: %d | Clusters: %d | Players: %d | Player Activity: %s | Under Deepslate: %s | Score: %d | Confidence: %d%%", score.geodes(), score.clusters(), score.players(), score.activityLabel(), score.underDeepslate() ? "YES" : "NO", score.total(), score.confidence());
    }

    private void announceIfChanged() {
        if (!chatSummary.get() || lastAnnouncement == qualified.size() || Minecraft.getInstance().player == null) return;
        lastAnnouncement = qualified.size();
        ChatUtils.info("Asian Rice Finder: %d high-activity chunks detected", qualified.size());
    }

    private void queue(int x, int z) { queue(new ChunkKey(x, z)); }
    private void queue(ChunkKey key) { if (queued.add(key)) queue.addLast(key); }
    private void clear() { cache.clear(); queue.clear(); queued.clear(); qualified.clear(); players.clear(); ticks = 0; lastAnnouncement = -1; }
}
