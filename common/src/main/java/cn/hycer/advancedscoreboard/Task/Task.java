package cn.hycer.advancedscoreboard.Task;

import static cn.hycer.advancedscoreboard.Global.Global.logger;
import static cn.hycer.advancedscoreboard.Global.Global.scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import cn.hycer.advancedscoreboard.mixin.ServerCommonPacketListenerImplAccessor;
import cn.hycer.advancedscoreboard.render.CustomScoreboardRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;

public class Task {

    private static final Map<UUID, Integer> playerRotationIndex = new HashMap<>();
    private static final Map<UUID, ScoreboardItem> playerCurrentItem = new HashMap<>();
    private static int tickCounter = 0;

    public static void onServerTick(MinecraftServer server) {
        tickCounter++;
        int switchIntervalTicks = Global.config.getSwitchInterval() * 20;
        int saveIntervalTicks = Global.config.getSaveInterval() * 20;

        if (tickCounter % 20 == 0) {
            syncLatency(server);
        }

        if (tickCounter % switchIntervalTicks == 0) {
            rotateDisplay(server);
        }

        if (tickCounter % saveIntervalTicks == 0) {
            syncDataToScoreboard(server);
            Global.config.saveConfig();
        }
    }

    private static void rotateDisplay(MinecraftServer server) {
        try {
            List<ScoreboardItem> allScoreboards = Global.config.getScoreboards();
            if (allScoreboards == null || allScoreboards.isEmpty()) {
                return;
            }

            Set<String> hidden = Global.config.getHiddenScoreboards();
            List<ScoreboardItem> visibleScoreboards = allScoreboards.stream()
                .filter(sb -> !hidden.contains(sb.getInternalName()))
                .toList();

            if (visibleScoreboards.isEmpty()) {
                return;
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID uuid = player.getUUID();
                int index = playerRotationIndex.getOrDefault(uuid, -1);
                index = (index + 1) % visibleScoreboards.size();
                playerRotationIndex.put(uuid, index);

                ScoreboardItem currentItem = visibleScoreboards.get(index);
                playerCurrentItem.put(uuid, currentItem);
                CustomScoreboardRenderer.sendDisplay(player, currentItem);
            }

            logger.debug("rotated per-player sidebars, showing {} scoreboards", visibleScoreboards.size());
        } catch (Exception e) {
            logger.error("scoreboard rotation error: {}", e.getMessage(), e);
        }
    }

    private static void syncDataToScoreboard(MinecraftServer server) {
        for (ScoreboardItem item : Global.config.getScoreboards()) {
            String internalName = item.getInternalName();
            if (Config.LATENCY_INTERNAL_NAME.equals(internalName)) continue;
            Objective objective = scoreboard.getObjective(internalName);
            if (objective == null) continue;
            switch (internalName) {
                case Config.ONLINE_TIME_INTERNAL_NAME -> {
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        int totalPlayTicks = player.getStats().getValue(Stats.CUSTOM, Stats.PLAY_TIME);
                        if (totalPlayTicks == 0) continue;
                        int totalHours = totalPlayTicks / 20 / 3600;
                        String playerName = player.getScoreboardName();
                        item.updateData(playerName, totalHours);
                    }
                }
                case Config.ELYTRA_DISTANCE_INTERNAL_NAME -> {
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        int aviateOneCM = player.getStats().getValue(Stats.CUSTOM, Stats.AVIATE_ONE_CM);
                        if (aviateOneCM == 0) continue;
                        int aviateOneKM = aviateOneCM / 100 / 1000;
                        String playerName = player.getScoreboardName();
                        item.updateData(playerName, aviateOneKM);
                    }
                }
                case Config.DAMAGE_TAKEN_INTERNAL_NAME -> {
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        int damageTaken = player.getStats().getValue(Stats.CUSTOM, Stats.DAMAGE_TAKEN) / 10;
                        if (damageTaken == 0) continue;
                        String playerName = player.getScoreboardName();
                        item.updateData(playerName, damageTaken);
                    }
                }
                case Config.DEATHS_INTERNAL_NAME -> {
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        int deaths = player.getStats().getValue(Stats.CUSTOM, Stats.DEATHS);
                        if (deaths == 0) continue;
                        String playerName = player.getScoreboardName();
                        item.updateData(playerName, deaths);
                    }
                }
            }

            syncTopNToScoreboard(objective, item.getData(), Global.config.getMaxDisplayNum());
        }
    }

    public static void syncTopNToScoreboard(Objective objective, Map<String, Integer> data, int maxDisplay) {
        List<Map.Entry<String, Integer>> topEntries = data.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(maxDisplay)
                .toList();

        Set<String> topPlayerNames = topEntries.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            if (!topPlayerNames.contains(entry.owner())) {
                scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly(entry.owner()), objective);
            }
        }

        for (Map.Entry<String, Integer> entry : topEntries) {
            ScoreHolder scoreHolder = ScoreHolder.forNameOnly(entry.getKey());
            ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(scoreHolder, objective);
            scoreAccess.set(entry.getValue());
        }
    }

    private static void syncLatency(MinecraftServer server) {
        ScoreboardItem item = Global.config.getScoreboardByInternalName(Config.LATENCY_INTERNAL_NAME);
        if (item == null) return;
        Objective objective = scoreboard.getObjective(Config.LATENCY_INTERNAL_NAME);
        if (objective == null) return;

        if (scoreboard.getDisplayObjective(DisplaySlot.LIST) != objective) {
            scoreboard.setDisplayObjective(DisplaySlot.LIST, objective);
        }

        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            if (!item.getData().containsKey(entry.owner())) {
                scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly(entry.owner()), objective);
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int pingMs = ((ServerCommonPacketListenerImplAccessor) player.connection).getLatency();
            item.updateData(player.getScoreboardName(), pingMs);
            ScoreHolder scoreHolder = ScoreHolder.forNameOnly(player.getScoreboardName());
            ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(scoreHolder, objective);
            scoreAccess.set(pingMs);
            Component display = Component.literal(pingMs + "ms")
                    .withStyle(Style.EMPTY.withColor(latencyColor(pingMs)));
            scoreAccess.numberFormatOverride(new FixedFormat(display));
        }
    }

    private static int latencyColor(int pingMs) {
        if (pingMs < 50)  return 0x55FF55;
        if (pingMs < 100) return 0xFFFF55;
        if (pingMs < 200) return 0xFFAA00;
        return 0xFF5555;
    }

    public static void removePlayer(UUID uuid) {
        playerRotationIndex.remove(uuid);
        playerCurrentItem.remove(uuid);
    }

    public static void refreshDisplayForItem(MinecraftServer server, ScoreboardItem item) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ScoreboardItem current = playerCurrentItem.get(player.getUUID());
            if (current != null && current.getInternalName().equals(item.getInternalName())) {
                CustomScoreboardRenderer.sendDisplay(player, item);
            }
        }
    }
}
