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
import cn.hycer.advancedscoreboard.mixin.ServerCommonPacketListenerImplAccessor;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.FixedNumberFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Style;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;

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

        // 延迟刷新（固定 1 秒）
        if (tickCounter % 20 == 0) {
            syncLatency(server);
        }

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
                .filter(sb -> !Config.LATENCY_INTERNAL_NAME.equals(sb.getInternalName()))
                .toList();

            if (visibleScoreboards.isEmpty()) {
                return;
            }

            rotationIndex = (rotationIndex + 1) % visibleScoreboards.size();
            ScoreboardItem currentItem = visibleScoreboards.get(rotationIndex);
            ScoreboardObjective objective = scoreboard.getNullableObjective(currentItem.getInternalName());
            if (objective != null) {
                logger.debug("rotating sidebar to '{}'", currentItem.getInternalName());
                scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
            } else {
                logger.warn("scoreboard objective is null for '{}', skipping display", currentItem.getInternalName());
            }
        } catch (Exception e) {
            logger.error("scoreboard rotation error: {}", e.getMessage(), e);
        }
    }

    /**
     * 延迟数据刷新（固定 1 秒周期，独立于其他数据同步）
     */
    private static void syncLatency(MinecraftServer server) {
        ScoreboardItem item = Global.config.getScoreboardByInternalName(Config.LATENCY_INTERNAL_NAME);
        if (item == null) return;
        ScoreboardObjective objective = scoreboard.getNullableObjective(Config.LATENCY_INTERNAL_NAME);
        if (objective == null) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            int pingMs = ((ServerCommonPacketListenerImplAccessor) player.networkHandler).getLatency();
            item.updateData(player.getName().getString(), pingMs);
        }
        syncLatencyToList(objective, item.getData());
    }

    // 数据同步逻辑
    private static void syncDataToScoreboard(MinecraftServer server) {
        for (ScoreboardItem item : Global.config.getScoreboards()) {
            String internalName = item.getInternalName();
            if (Config.LATENCY_INTERNAL_NAME.equals(internalName)) continue;
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
     * 同步延迟数据到 LIST 显示（TAB 玩家列表），每个玩家显示带 " ms" 单位的延迟值。
     * 同时移除已离线的玩家条目。
     */
    private static void syncLatencyToList(ScoreboardObjective objective, Map<String, Integer> data) {
        // 确保 LIST 显示槽始终指向延迟 objective
        if (scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.LIST) != objective) {
            scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.LIST, objective);
        }

        // 清除离线玩家（data 中只包含刚同步的在线玩家）
        for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
            if (!data.containsKey(entry.owner())) {
                scoreboard.removeScore(ScoreHolder.fromName(entry.owner()), objective);
            }
        }

        // 同步在线玩家延迟（带颜色 + 单位）
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            String playerName = entry.getKey();
            int pingMs = entry.getValue();
            ScoreHolder scoreHolder = ScoreHolder.fromName(playerName);
            ScoreAccess scoreAccess = scoreboard.getOrCreateScore(scoreHolder, objective);
            scoreAccess.setScore(pingMs);
            Text display = Text.literal(pingMs + " ms")
                    .setStyle(Style.EMPTY.withColor(latencyColor(pingMs)));
            scoreAccess.setNumberFormat(new FixedNumberFormat(display));
        }
    }

    /**
     * 根据延迟值返回对应的显示颜色。
     * < 50ms 绿色 | < 100ms 黄色 | < 200ms 金色 | >= 200ms 红色
     */
    private static int latencyColor(int pingMs) {
        if (pingMs < 50)  return 0x55FF55; // 绿色
        if (pingMs < 100) return 0xFFFF55; // 黄色
        if (pingMs < 200) return 0xFFAA00; // 金色
        return 0xFF5555;                   // 红色
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

}
