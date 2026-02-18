package cn.hycer.advancedscoreboard.Event;

import cn.hycer.advancedscoreboard.Config.Config;
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

import static cn.hycer.advancedscoreboard.Global.Global.scoreboard;

public class ServerStartedEvent {
    public static void onServerStarted(MinecraftServer server) {

        // 注册挖掘榜
        registerMineCount(server);
        // 注册在线时长榜
        registerOnlineTime(server);
        // 启动计分板轮播任务
        startScoreboardSwitchTask(server);
        // 启动在线时长统计任务
        startOnlineTimeCounter(server);
    }

    // 注册挖掘榜
    public static void registerMineCount(MinecraftServer server) {
//        String mineCountInternalName = Global.config.getValue(Config.MiningInternalNameConfigKey);
//        String mineCountDisplayName = Global.config.getValue(Config.MiningDisplayNameConfigKey);
        String mineCountInternalName = "mine_count";
        String mineCountDisplayName = "挖掘榜";

        // 初始化全局scoreboard变量
        scoreboard = Objects.requireNonNull(server.getWorld(World.OVERWORLD).getScoreboard());
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
//        String onlineTimeInternalName = Global.config.getValue(Config.OnlineTimeInternalNameConfigKey);
//        String onlineTimeDisplayName = Global.config.getValue(Config.OnlineTimeDisplayNameConfigKey);
        String onlineTimeInternalName = "online_time";
        String onlineTimeDisplayName = "在线时长";

        ScoreboardObjective onlineTimeScoreboardObj = scoreboard.getNullableObjective(onlineTimeInternalName);

        if (onlineTimeScoreboardObj != null) {
            onlineTimeScoreboardObj.setDisplayName(Text.literal(onlineTimeDisplayName));
            return;
        }

        onlineTimeScoreboardObj = scoreboard.addObjective(
                onlineTimeInternalName,
                ScoreboardCriterion.DUMMY,
                Text.literal(onlineTimeDisplayName),
                ScoreboardCriterion.RenderType.INTEGER,
                true,
                null
        );
    }

    // 在轮播切换任务中添加格式化
    public static void startScoreboardSwitchTask(MinecraftServer server) {
        int interval = 5 * 20; // 5秒切换（可从配置读取）
        server.submit(() -> {
            new Thread(() -> {
                while (!server.isStopped()) {
                    try {
                        Thread.sleep(interval * 50);
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

    // 启动在线时长统计任务（按整数小时存储）
    public static void startOnlineTimeCounter(MinecraftServer server) {
        server.submit(() -> {
            new Thread(() -> {
                // 每个玩家的秒数累计器（避免重启后丢失，若需持久化可存到配置/数据库）
                final Map<ServerPlayerEntity, Integer> playerSecondCounter = new ConcurrentHashMap<>();

                while (!server.isStopped()) {
                    try {
                        Thread.sleep(1000); // 每秒执行一次
                        server.execute(() -> {
                            String onlineTimeInternalName = "online_time";
                            ScoreboardObjective onlineTimeObj = scoreboard.getNullableObjective(onlineTimeInternalName);

                            if (onlineTimeObj == null) return;

                            // 遍历所有在线玩家更新时长（仅整数小时存储）
                            server.getPlayerManager().getPlayerList().forEach(player -> {
                                if (player.isAlive() && !player.isSpectator()) {
                                    // 1. 获取该玩家当前累计的秒数（初始为0）
                                    int currentSeconds = playerSecondCounter.getOrDefault(player, 0);
                                    System.out.println("current: " + currentSeconds);
                                    // 2. 秒数+1
                                    currentSeconds += 1;
                                    // 3. 更新秒数累计器
                                    playerSecondCounter.put(player, currentSeconds);

                                    // 4. 核心逻辑：仅当秒数达到3600的整数倍时，更新小时数
                                    if (currentSeconds % 3600 == 0) {
                                        var scoreAccess = scoreboard.getOrCreateScore(player, onlineTimeObj);
                                        // 当前存储的小时数 +1（直接存储整数小时）
                                        int currentHours = scoreAccess.getScore();
                                        scoreAccess.setScore(currentHours + 1);

                                        // 可选：控制台日志，验证小时数更新
                                        System.out.println("玩家" + player.getName().getString() +
                                                " 在线时长累计1小时，当前总时长：" + (currentHours + 1) + "小时");
                                    }
                                } else {
                                    // 玩家离线/旁观/死亡时，清空该玩家的秒数累计器（可选）
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
}