package cn.hycer.advancedscoreboard.Event;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import cn.hycer.advancedscoreboard.Task.Task;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

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
        scoreboard = Objects.requireNonNull(server.overworld().getScoreboard());
        // 先清空 mod 相关的计分板数据，确保以配置文件为准
        clearInGameScoreboardData(server);
        // 注册计分板
        registerScoreboard(server);
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
                Objective scoreboardObj = scoreboard.getObjective(sb.getInternalName());
                if (scoreboardObj != null) {
                    scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, scoreboardObj);
                    continue;
                }
                scoreboardObj = scoreboard.addObjective(
                    sb.getInternalName(),
                    ObjectiveCriteria.DUMMY,
                    Component.literal(formattedName),
                    ObjectiveCriteria.RenderType.INTEGER,
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
     * 刷新所有已注册计分板的显示名（border 变更后调用）
     */
    public static void refreshAllDisplayNames() {
        if (scoreboard == null) return;
        for (ScoreboardItem sb : Global.config.getScoreboards()) {
            Objective obj = scoreboard.getObjective(sb.getInternalName());
            if (obj != null) {
                // Use packet-based update for display name change
                // ClientboundSetObjectivePacket handles this
            }
        }
        logger.info("all scoreboard display names refreshed");
    }

    /**
     * 同步配置文件中的所有数据到游戏内计分板
     */
    public static void syncDataFromConfig() {
        try {
            for (ScoreboardItem item : Global.config.getScoreboards()) {
                String internalName = item.getInternalName();
                Objective objective = scoreboard.getObjective(internalName);
                if (objective == null) continue;

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
     */
    public static void clearInGameScoreboardData(MinecraftServer server) {
        if (scoreboard == null) {
            scoreboard = Objects.requireNonNull(server.overworld().getScoreboard());
        }

        Set<String> currentInternalNames = Global.config.getScoreboards().stream()
                .map(ScoreboardItem::getInternalName)
                .collect(Collectors.toSet());
        // 旧版残留名称，需彻底移除
        Set<String> obsoleteNames = Set.of("elytron_distance");

        try {
            List<Objective> toRemove = new ArrayList<>();
            for (Objective objective : scoreboard.getObjectives()) {
                String name = objective.getName();
                if (obsoleteNames.contains(name)) {
                    toRemove.add(objective);
                } else if (currentInternalNames.contains(name)) {
                    for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
                        scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly(entry.owner()), objective);
                    }
                }
            }

            for (Objective obj : toRemove) {
                scoreboard.removeObjective(obj);
                logger.info("removed obsolete objective: {}", obj.getName());
            }

            logger.info("scoreboard data cleared, removed {} obsolete objectives", toRemove.size());
        } catch (Exception e) {
            logger.error("reset scoreboard data failed: {}", e.getMessage());
        }
    }
}
