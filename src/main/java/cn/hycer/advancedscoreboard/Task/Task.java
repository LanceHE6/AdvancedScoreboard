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
import cn.hycer.advancedscoreboard.render.CustomScoreboardRenderer;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;

public class Task {

    // 每玩家的轮播索引
    private static final Map<UUID, Integer> playerRotationIndex = new HashMap<>();

    // 每个玩家当前正在显示的 ScoreboardItem
    private static final Map<UUID, ScoreboardItem> playerCurrentItem = new HashMap<>();

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

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID uuid = player.getUuid();
                int index = playerRotationIndex.getOrDefault(uuid, -1);
                index = (index + 1) % visibleScoreboards.size();
                playerRotationIndex.put(uuid, index);

                ScoreboardItem currentItem = visibleScoreboards.get(index);
                playerCurrentItem.put(uuid, currentItem);
                CustomScoreboardRenderer.sendDisplay(player, currentItem);
            }

            logger.debug("rotated per-player sidebars, showing '{}'", visibleScoreboards.size());
        } catch (Exception e) {
            logger.error("scoreboard rotation error: {}", e.getMessage(), e);
        }
    }

    // 数据同步逻辑
    private static void syncDataToScoreboard(MinecraftServer server) {
        for (ScoreboardItem item : Global.config.getScoreboards()) {
            String internalName = item.getInternalName();
            ScoreboardObjective objective = scoreboard.getNullableObjective(internalName);
            if (objective == null) continue;
            switch (internalName) {
                case Config.ONLINE_TIME_INTERNAL_NAME -> {
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        int totalPlayTicks = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
                        if (totalPlayTicks == 0) continue;
                        int totalHours = totalPlayTicks / 20 / 3600;
                        String playerName = player.getName().getString();
                        item.updateData(playerName, totalHours);
                    }
                }
                case Config.ELYTRA_DISTANCE_INTERNAL_NAME -> {
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        int aviateOneCM = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.AVIATE_ONE_CM));
                        if (aviateOneCM == 0) continue;
                        int aviateOneKM = aviateOneCM / 100 / 1000;
                        String playerName = player.getName().getString();
                        item.updateData(playerName, aviateOneKM);
                    }
                }
                case Config.DAMAGE_TAKEN_INTERNAL_NAME -> {
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        int damageTaken = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN)) / 10;
                        if (damageTaken == 0) continue;
                        String playerName = player.getName().getString();
                        item.updateData(playerName, damageTaken);
                    }
                }
                case Config.DEATHS_INTERNAL_NAME -> {
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        int deaths = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DEATHS));
                        if (deaths == 0) continue;
                        String playerName = player.getName().getString();
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
    public static void syncTopNToScoreboard(ScoreboardObjective objective, Map<String, Integer> data, int maxDisplay) {
        List<Map.Entry<String, Integer>> topEntries = data.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(maxDisplay)
                .toList();

        Set<String> topPlayerNames = topEntries.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
            if (!topPlayerNames.contains(entry.owner())) {
                scoreboard.removeScore(ScoreHolder.fromName(entry.owner()), objective);
            }
        }

        for (Map.Entry<String, Integer> entry : topEntries) {
            ScoreHolder scoreHolder = ScoreHolder.fromName(entry.getKey());
            ScoreAccess scoreAccess = scoreboard.getOrCreateScore(scoreHolder, objective);
            scoreAccess.setScore(entry.getValue());
        }
    }

    public static void removePlayer(UUID uuid) {
        playerRotationIndex.remove(uuid);
        playerCurrentItem.remove(uuid);
    }

    /**
     * 刷新所有正在观看指定榜单的玩家的显示（实时事件更新后调用）
     */
    public static void refreshDisplayForItem(MinecraftServer server, ScoreboardItem item) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ScoreboardItem current = playerCurrentItem.get(player.getUuid());
            if (current != null && current.getInternalName().equals(item.getInternalName())) {
                CustomScoreboardRenderer.sendDisplay(player, item);
            }
        }
    }

}
