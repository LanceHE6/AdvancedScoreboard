package cn.hycer.advancedscoreboard.Event;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import net.minecraft.scoreboard.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

import static cn.hycer.advancedscoreboard.Global.Global.logger;
import static cn.hycer.advancedscoreboard.Global.Global.scoreboard;

public class ServerStartedEvent {
    // 配置同步间隔（5秒）
    private static final long CONFIG_SYNC_INTERVAL = 5 * 1000;

    public static void onServerStarted(MinecraftServer server) {
        // 初始化全局scoreboard变量
        scoreboard = Objects.requireNonNull(server.getWorld(World.OVERWORLD).getScoreboard());
        // 重置服务器计分板数据
        clearInGameScoreboardData(server);
        // 注册计分板
        registerScoreboard(server);
        // 启动计分板轮播任务
        startScoreboardSwitchTask(server);
        // 启动在线时长统计任务
        startOnlineTimeCounter(server);
        // 新增：启动配置同步任务（5秒读取配置→更新计分板→保存数据）
        startConfigSyncTask(server);
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

    // 轮播切换任务
    public static void startScoreboardSwitchTask(MinecraftServer server) {
        // 从JSON配置读取轮播间隔
        int intervalSeconds = Global.config.getSwitchInterval();
        long intervalMs = intervalSeconds * 1000;

        server.submit(() -> {
            new Thread(() -> {
                while (!server.isStopped()) {
                    try {
                        Thread.sleep(intervalMs);
                        server.execute(() -> {
                            if (Global.isShowingMining) {
                                // 切换到在线时长榜
                                ScoreboardObjective onlineTimeObj = scoreboard.getNullableObjective("online_time");
                                if (onlineTimeObj != null) {
                                    scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, onlineTimeObj);
                                }
                            } else {
                                // 切换到挖掘榜
                                ScoreboardObjective miningObj = scoreboard.getNullableObjective("mine_count");
                                if (miningObj != null) {
                                    scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, miningObj);
                                }
                            }
                            Global.isShowingMining = !Global.isShowingMining;
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
        });
    }

    // 在线时长计时器
    public static void startOnlineTimeCounter(MinecraftServer server) {
        server.submit(() -> {
            new Thread(() -> {
                while (!server.isStopped()) {
                    try {
                        Thread.sleep(1000); // 每秒执行一次
                        server.execute(() -> {
                            ScoreboardObjective onlineTimeObj = scoreboard.getNullableObjective(Config.ONLINE_TIME_INTERNAL_NAME);

                            if (onlineTimeObj == null) return;

                            // 遍历所有在线玩家更新时长
                            server.getPlayerManager().getPlayerList().forEach(player -> {
                                if (player.isAlive() && !player.isSpectator()) {
                                    String name = player.getName().getString();
                                    int currentSeconds = Global.config.getScoreboardByInternalName(Config.ONLINE_TIME_INTERNAL_NAME)
                                            .getDataValue(name, 0);
                                    Global.config.getScoreboardByInternalName(Config.ONLINE_TIME_INTERNAL_NAME).updateData(name, ++currentSeconds);
                                }
                            });
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
        });
    }

    // 同步配置（读取配置→更新计分板→保存数据）
    public static void startConfigSyncTask(MinecraftServer server) {
        server.submit(() -> {
            new Thread(() -> {
                while (!server.isStopped()) {
                    try {
                        Thread.sleep(CONFIG_SYNC_INTERVAL); // 每隔5秒执行
                        server.execute(() -> {
                            try {
                                // 遍历所有计分板，将配置数据同步到游戏内计分板
                                syncConfigToScoreboard();
                                logger.trace("scoreboard updated");

                                // 将数据保存至本地
                                Global.config.saveConfig(); // 保存到本地文件
                                logger.trace("config updated");

                            } catch (Exception e) {
                                logger.error("config sync failed： {}", e.getMessage());
                            }
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
        });
    }

    // 将JSON配置中的数据同步到游戏内计分板
    private static void syncConfigToScoreboard() {
        // 遍历所有配置的计分板
        for (ScoreboardItem item : Global.config.getScoreboards()) {
            String internalName = item.getInternalName();
            ScoreboardObjective objective = scoreboard.getNullableObjective(internalName);
            if (objective == null) continue;


            // 遍历配置中的玩家数据，更新到计分板
            for (Map.Entry<String, Integer> entry : item.getData().entrySet()) {
                String playerName = entry.getKey();
                int scoreValue;
                // 在线时长榜单特殊处理，以小时显示
                if (Objects.equals(internalName, Config.ONLINE_TIME_INTERNAL_NAME)) {
                    scoreValue = entry.getValue() / 3600;
                } else {
                    scoreValue = entry.getValue();
                }
                // 根据name获取ScoreHolder
                ScoreHolder scoreHolder = ScoreHolder.fromName(playerName);
                // 更新计分板分数
                ScoreAccess scoreAccess = scoreboard.getOrCreateScore(scoreHolder, objective);
                scoreAccess.setScore(scoreValue);
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
