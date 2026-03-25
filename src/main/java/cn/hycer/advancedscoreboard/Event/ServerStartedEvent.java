package cn.hycer.advancedscoreboard.Event;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import cn.hycer.advancedscoreboard.Task.Task;
import net.minecraft.scoreboard.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.Objects;
import java.util.List;

import static cn.hycer.advancedscoreboard.Global.Global.logger;
import static cn.hycer.advancedscoreboard.Global.Global.scoreboard;

public class ServerStartedEvent {

    public static void onServerStarted(MinecraftServer server) {
        // 初始化全局scoreboard变量
        scoreboard = Objects.requireNonNull(server.getWorld(World.OVERWORLD).getScoreboard());
        // 重置服务器计分板数据
        clearInGameScoreboardData(server);
        // 注册计分板
        registerScoreboard(server);
        // 启动计分板轮播任务
        Task.scoreboardSwitch(server);
        // 启动更新计分板任务
        Task.syncData(server);
    }

    public static void registerScoreboard(MinecraftServer server) {
        List<ScoreboardItem> scoreboards = Global.config.getScoreboards();
        for (ScoreboardItem sb : scoreboards) {
            ScoreboardObjective scoreboardObj = scoreboard.getNullableObjective(sb.getInternalName());
            if (scoreboardObj != null) {
                scoreboardObj.setDisplayName(Text.literal(sb.getDisplayName()));
                continue;
            }
            scoreboardObj = scoreboard.addObjective(
                sb.getInternalName(),
                ScoreboardCriterion.DUMMY,
                Text.literal(sb.getDisplayName()),
                ScoreboardCriterion.RenderType.INTEGER,
                true,
                null
            );
            logger.debug("registered: {}", sb.getInternalName());
            // 默认显示挖掘榜
            if (Objects.equals(sb.getInternalName(), Config.MINE_COUNT_INTERNAL_NAME)) {
                scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, scoreboardObj);
            }
        }
    }

    /**
     * 清空服务器内所有游戏内计分板的玩家数据
     * 用于服务器启动时初始化，确保游戏内数据与配置文件一致
     * @param server MinecraftServer实例
     */
    public static void clearInGameScoreboardData(MinecraftServer server) {
        // 确保scoreboard实例初始化
        if (scoreboard == null) {
            scoreboard = Objects.requireNonNull(server.getWorld(World.OVERWORLD).getScoreboard());
        }

        try {
            // 获取所有已注册的计分板对象
            Collection<ScoreboardObjective> allObjectives = scoreboard.getObjectives();
            if (allObjectives.isEmpty()) {
                return;
            }

            // 遍历清空每个计分板的所有玩家数据
            for (ScoreboardObjective objective : allObjectives) {
                Collection<ScoreboardEntry> scoreEntries = scoreboard.getScoreboardEntries(objective);

                // 逐个移除玩家分数
                for (ScoreboardEntry entry : scoreEntries) {
                    ScoreHolder scoreHolder = ScoreHolder.fromName(entry.owner());
                    scoreboard.removeScore(scoreHolder, objective);
                }

                logger.info("reset scoreboard data success");
            }

        } catch (Exception e) {
            logger.error("reset scoreboard data failed: {}", e.getMessage());
        }
    }
}
