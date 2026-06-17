package cn.hycer.advancedscoreboard.Task;

import static cn.hycer.advancedscoreboard.Global.Global.logger;
import static cn.hycer.advancedscoreboard.Global.Global.scoreboard;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Stats;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;

public class Task {

    // 全局轮播索引
    private static int rotationIndex = 0;

    // Tick 计数器
    private static int tickCounter = 0;

    // 轮播切换（由 ServerTick 事件驱动）
    public static void onServerTick(MinecraftServer server) {
        tickCounter++;
        int switchIntervalTicks = Global.config.getSwitchInterval() * 20;
        int saveIntervalTicks = Global.config.getSaveInterval() * 20;

        // 轮播切换
        if (tickCounter % switchIntervalTicks == 0) {
            rotateDisplay(server);
        }

        // 数据同步
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

            rotationIndex = (rotationIndex + 1) % visibleScoreboards.size();
            ScoreboardItem currentItem = visibleScoreboards.get(rotationIndex);
            Objective objective = scoreboard.getObjective(currentItem.getInternalName());
            if (objective != null) {
                logger.debug("rotating sidebar to '{}'", currentItem.getInternalName());
                scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
            } else {
                logger.warn("scoreboard objective is null for '{}', skipping display", currentItem.getInternalName());
            }
        } catch (Exception e) {
            logger.error("scoreboard rotation error: {}", e.getMessage(), e);
        }
    }

    // 数据同步逻辑
    private static void syncDataToScoreboard(MinecraftServer server) {
        for (ScoreboardItem item : Global.config.getScoreboards()) {
            String internalName = item.getInternalName();
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

    /**
     * 将数据按分数降序排列后，仅同步前 maxDisplay 名玩家到游戏内计分板
     * 同时移除不在前 N 名的玩家条目
     */
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

}
