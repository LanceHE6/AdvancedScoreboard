package cn.hycer.advancedscoreboard.Event;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import net.minecraft.scoreboard.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static cn.hycer.advancedscoreboard.Global.Global.scoreboard;

public class ServerStartedEvent {
    // 配置同步间隔（5秒）
    private static final long CONFIG_SYNC_INTERVAL = 5 * 1000;

    public static void onServerStarted(MinecraftServer server) {
        // 初始化全局scoreboard变量
        scoreboard = Objects.requireNonNull(server.getWorld(World.OVERWORLD).getScoreboard());
        // 清空服务器计分板数据
        clearInGameScoreboardData(server);
        // 注册挖掘榜
        registerMineCount(server);
        // 注册在线时长榜
        registerOnlineTime(server);
        // 启动计分板轮播任务
        startScoreboardSwitchTask(server);
        // 启动在线时长统计任务
        startOnlineTimeCounter(server);
        // 新增：启动配置同步任务（5秒读取配置→更新计分板→保存数据）
        startConfigSyncTask(server);
    }

    // 注册挖掘榜
    public static void registerMineCount(MinecraftServer server) {
        String mineCountInternalName = "mine_count"; // 对应JSON配置中的internalName
        // 从JSON配置读取显示名
        ScoreboardItem mineCountItem = Global.config.getScoreboardByInternalName(mineCountInternalName);
        String mineCountDisplayName = mineCountItem != null ? mineCountItem.getDisplayName() : "挖掘榜";

        ScoreboardObjective mineCountScoreboardObj = scoreboard.getNullableObjective(mineCountInternalName);

        //判断获取到的计分板对象是否为空
        if (mineCountScoreboardObj != null) {
            mineCountScoreboardObj.setDisplayName(Text.literal(mineCountDisplayName));
            return;
        }

        mineCountScoreboardObj = scoreboard.addObjective(
                mineCountInternalName,
                ScoreboardCriterion.DUMMY,
                Text.literal(mineCountDisplayName),
                ScoreboardCriterion.RenderType.INTEGER,
                true,
                null
        );
        // 默认显示挖掘榜
        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, mineCountScoreboardObj);
    }

    // 注册在线时长榜
    public static void registerOnlineTime(MinecraftServer server) {
        String onlineTimeInternalName = "online_time"; // 对应JSON配置中的internalName
        // 从JSON配置读取显示名
        ScoreboardItem onlineTimeItem = Global.config.getScoreboardByInternalName(onlineTimeInternalName);
        String onlineTimeDisplayName = onlineTimeItem != null ? onlineTimeItem.getDisplayName() : "在线时长";

        ScoreboardObjective onlineTimeScoreboardObj = scoreboard.getNullableObjective(onlineTimeInternalName);

        if (onlineTimeScoreboardObj != null) {
            onlineTimeScoreboardObj.setDisplayName(Text.literal(onlineTimeDisplayName));
            return;
        }

        onlineTimeScoreboardObj = scoreboard.addObjective(
                onlineTimeInternalName,
                ScoreboardCriterion.DUMMY,
                Text.literal(onlineTimeDisplayName),
                null, // 关键：取消INTEGER渲染，支持自定义显示
                true,
                null
        );
    }

    // 轮播切换任务
    public static void startScoreboardSwitchTask(MinecraftServer server) {
        // 从JSON配置读取轮播间隔（秒→毫秒）
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

    // 在线时长统计任务（按整数小时存储）
    public static void startOnlineTimeCounter(MinecraftServer server) {
        server.submit(() -> {
            new Thread(() -> {
                // 每个玩家的秒数累计器
                final Map<ServerPlayerEntity, Integer> playerSecondCounter = new ConcurrentHashMap<>();

                while (!server.isStopped()) {
                    try {
                        Thread.sleep(1000); // 每秒执行一次
                        server.execute(() -> {
                            String onlineTimeInternalName = "online_time";
                            ScoreboardObjective onlineTimeObj = scoreboard.getNullableObjective(onlineTimeInternalName);

                            if (onlineTimeObj == null) return;

                            // 遍历所有在线玩家更新时长
                            server.getPlayerManager().getPlayerList().forEach(player -> {
                                if (player.isAlive() && !player.isSpectator()) {
                                    // 1. 获取该玩家当前累计的秒数
                                    int currentSeconds = playerSecondCounter.getOrDefault(player, 0);
                                    // 2. 秒数+1
                                    currentSeconds += 1;
                                    // 3. 更新秒数累计器
                                    playerSecondCounter.put(player, currentSeconds);

                                    // 4. 满1小时更新小时数
                                    if (currentSeconds % 3600 == 0) {
                                        var scoreAccess = scoreboard.getOrCreateScore(player, onlineTimeObj);
                                        int currentHours = scoreAccess.getScore();
                                        scoreAccess.setScore(currentHours + 1);

                                        // 同步更新到JSON配置（关键：内存中先更新）
                                        updateScoreboardDataToConfig(
                                                onlineTimeInternalName,
                                                player.getUuidAsString(),
                                                currentHours + 1
                                        );

                                        System.out.println("玩家" + player.getName().getString() +
                                                " 在线时长累计1小时，当前总时长：" + (currentHours + 1) + "小时");
                                    }
                                } else {
                                    // 玩家离线/旁观/死亡时清空累计器
                                    playerSecondCounter.remove(player);
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

    // 新增：5秒同步配置任务（读取配置→更新计分板→保存数据）
    public static void startConfigSyncTask(MinecraftServer server) {
        server.submit(() -> {
            new Thread(() -> {
                while (!server.isStopped()) {
                    try {
                        Thread.sleep(CONFIG_SYNC_INTERVAL); // 每隔5秒执行
                        server.execute(() -> {
                            try {
                                // 步骤2：遍历所有计分板，将配置数据同步到游戏内计分板
                                syncConfigToScoreboard();
                                System.out.println("已更新计分板");

                                // 步骤3:将数据保存至本地
                                Global.config.saveConfig(); // 保存到本地文件
                                System.out.println("已将计分板数据保存到JSON配置文件");

                            } catch (Exception e) {
                                System.err.println("配置同步失败：" + e.getMessage());
                                e.printStackTrace();
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

    // 辅助方法：将JSON配置中的数据同步到游戏内计分板
    private static void syncConfigToScoreboard() {
        // 遍历所有配置的计分板
        for (ScoreboardItem item : Global.config.getScoreboards()) {
            String internalName = item.getInternalName();
            ScoreboardObjective objective = scoreboard.getNullableObjective(internalName);
            if (objective == null) continue;

            // 遍历配置中的玩家数据，更新到计分板
            for (Map.Entry<String, Integer> entry : item.getData().entrySet()) {
                String playerName = entry.getKey();
                int scoreValue = entry.getValue();

                // 根据UUID获取ScoreHolder
                ScoreHolder scoreHolder = ScoreHolder.fromName(playerName);
                // 更新计分板分数
                ScoreAccess scoreAccess = scoreboard.getOrCreateScore(scoreHolder, objective);
                scoreAccess.setScore(scoreValue);
            }
        }
    }

    // 辅助方法：更新单个玩家的计分板数据到配置（内存中）
    private static void updateScoreboardDataToConfig(String internalName, String playerName, int value) {
        ScoreboardItem item = Global.config.getScoreboardByInternalName(internalName);
        if (item != null) {
            item.updateData(playerName, value);
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
            // 1. 获取所有已注册的计分板对象
            Collection<ScoreboardObjective> allObjectives = scoreboard.getObjectives();
            if (allObjectives.isEmpty()) {
                System.out.println("[计分板初始化] 游戏内暂无计分板数据，无需清空");
                return;
            }

            // 2. 遍历清空每个计分板的所有玩家数据
            int totalCleared = 0;
            for (ScoreboardObjective objective : allObjectives) {
                Collection<ScoreboardEntry> scoreEntries = scoreboard.getScoreboardEntries(objective);
                int clearedCount = 0;

                // 逐个移除玩家的计分板分数（仅游戏内）
                for (ScoreboardEntry entry : scoreEntries) {
                    ScoreHolder scoreHolder = ScoreHolder.fromName(entry.owner());
                    scoreboard.removeScore(scoreHolder, objective);
                    clearedCount++;
                }

                totalCleared += clearedCount;
                System.out.printf("[计分板初始化] 清空游戏内计分板「%s」的 %d 条数据%n",
                        objective.getName(), clearedCount);
            }

            System.out.printf("[计分板初始化] 完成！总计清空 %d 条游戏内计分板数据（配置文件未修改）%n", totalCleared);

        } catch (Exception e) {
            System.err.println("[计分板初始化] 清空游戏内数据失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
