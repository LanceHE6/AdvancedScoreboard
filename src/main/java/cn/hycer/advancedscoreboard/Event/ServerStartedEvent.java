package cn.hycer.advancedscoreboard.Event;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import cn.hycer.advancedscoreboard.Task.Task;
import net.minecraft.scoreboard.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.hycer.advancedscoreboard.Global.Global.logger;
import static cn.hycer.advancedscoreboard.Global.Global.scoreboard;

public class ServerStartedEvent {

    public static void onServerStarted(MinecraftServer server) {
        // 初始化全局scoreboard变量
        scoreboard = Objects.requireNonNull(server.getWorld(World.OVERWORLD).getScoreboard());
        // 先清空 mod 相关的计分板数据，确保以配置文件为准
        clearInGameScoreboardData(server);
        // 注册计分板
        registerScoreboard(server);
        // 将延迟 objective 固定在 LIST 显示槽（TAB 玩家列表）
        setLatencyToListDisplay();
        // 同步配置文件中的所有数据到游戏内计分板
        syncDataFromConfig();
        // 注册 ServerTick 事件驱动轮播和数据同步
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(Task::onServerTick);
    }

    public static void registerScoreboard(MinecraftServer server) {
        List<ScoreboardItem> scoreboards = Global.config.getScoreboards();
        for (ScoreboardItem sb : scoreboards) {
            try {
                String formattedName = Global.config.getFormattedDisplayName(sb);
                ScoreboardObjective scoreboardObj = scoreboard.getNullableObjective(sb.getInternalName());
                if (scoreboardObj != null) {
                    scoreboardObj.setDisplayName(Text.literal(formattedName));
                    continue;
                }
                scoreboardObj = scoreboard.addObjective(
                    sb.getInternalName(),
                    ScoreboardCriterion.DUMMY,
                    Text.literal(formattedName),
                    ScoreboardCriterion.RenderType.INTEGER,
                    true,
                    null
                );
                if (scoreboardObj != null) {
                    logger.info("registered scoreboard: {}", sb.getInternalName());
                } else {
                    logger.error("failed to register scoreboard: {} (addObjective returned null)", sb.getInternalName());
                }
            } catch (Exception e) {
                logger.error("failed to register scoreboard: {}, reason: {}", sb.getInternalName(), e.getMessage());
            }
        }
    }

    /**
     * 将延迟 objective 固定显示在 LIST 槽位（TAB 玩家列表），独立于 SIDEBAR 轮播。
     */
    private static void setLatencyToListDisplay() {
        if (scoreboard == null) return;
        ScoreboardObjective latencyObj = scoreboard.getNullableObjective(Config.LATENCY_INTERNAL_NAME);
        if (latencyObj != null) {
            scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.LIST, latencyObj);
            logger.info("latency objective set to LIST display slot");
        }
    }

    /**
     * 刷新所有已注册计分板的显示名（border 变更后调用）
     */
    public static void refreshAllDisplayNames() {
        if (scoreboard == null) return;
        for (ScoreboardItem sb : Global.config.getScoreboards()) {
            ScoreboardObjective obj = scoreboard.getNullableObjective(sb.getInternalName());
            if (obj != null) {
                obj.setDisplayName(Text.literal(Global.config.getFormattedDisplayName(sb)));
            }
        }
        logger.info("all scoreboard display names refreshed");
    }

    /**
     * 同步配置文件中的所有数据到游戏内计分板
     */
    public static void syncDataFromConfig() {
        try {
            // 遍历所有配置的计分板
            for (ScoreboardItem item : Global.config.getScoreboards()) {
                String internalName = item.getInternalName();
                ScoreboardObjective objective = scoreboard.getNullableObjective(internalName);
                if (objective == null) continue;
                
                // 按 maxDisplayNum 限制同步玩家数据到游戏计分板
                Task.syncTopNToScoreboard(objective, item.getData(), Global.config.getMaxDisplayNum());
            }
            logger.info("synced all player data from config to scoreboard");
        } catch (Exception e) {
            logger.error("sync data from config failed: {}", e.getMessage());
        }
    }

    /**
     * 清空 mod 相关的游戏内计分板数据
     * 确保 game scoreboard 与配置文件一致，同时清理不再使用的旧 objective
     * @param server MinecraftServer实例
     */
    public static void clearInGameScoreboardData(MinecraftServer server) {
        if (scoreboard == null) {
            scoreboard = Objects.requireNonNull(server.getWorld(World.OVERWORLD).getScoreboard());
        }

        Set<String> currentInternalNames = Global.config.getScoreboards().stream()
                .map(ScoreboardItem::getInternalName)
                .collect(Collectors.toSet());
        // 旧版残留名称，需彻底移除
        Set<String> obsoleteNames = Set.of("elytron_distance");

        try {
            List<ScoreboardObjective> toRemove = new ArrayList<>();
            for (ScoreboardObjective objective : scoreboard.getObjectives()) {
                String name = objective.getName();
                if (obsoleteNames.contains(name)) {
                    // 旧版残留，移除整个 objective
                    toRemove.add(objective);
                } else if (currentInternalNames.contains(name)) {
                    // 当前在用的 objective，清空分数以便从配置文件重新同步
                    for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
                        scoreboard.removeScore(ScoreHolder.fromName(entry.owner()), objective);
                    }
                }
            }

            for (ScoreboardObjective obj : toRemove) {
                scoreboard.removeObjective(obj);
                logger.info("removed obsolete objective: {}", obj.getName());
            }

            logger.info("scoreboard data cleared, removed {} obsolete objectives", toRemove.size());
        } catch (Exception e) {
            logger.error("reset scoreboard data failed: {}", e.getMessage());
        }
    }
}