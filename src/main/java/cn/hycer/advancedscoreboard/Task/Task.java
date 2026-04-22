package cn.hycer.advancedscoreboard.Task;

import static cn.hycer.advancedscoreboard.Global.Global.logger;
import static cn.hycer.advancedscoreboard.Global.Global.scoreboard;

import java.util.List;
import java.util.Map;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;

public class Task {

    // 轮播切换任务
    public static void scoreboardSwitch(MinecraftServer server) {
        server.submit(() -> {
            new Thread(() -> {
                // 当前显示的计分板索引
                final int[] index = {0};

                while (!server.isStopped()) {
                    try {
                        // 每次循环都从配置读取间隔，支持热修改
                        int intervalSeconds = Global.config.getSwitchInterval();
                        long intervalMs = (long) intervalSeconds * 1000;
                        Thread.sleep(intervalMs);

                        server.execute(() -> {
                            // 获取配置中的所有计分板列表
                            List<ScoreboardItem> scoreboards = Global.config.getScoreboards();
                            if (scoreboards == null || scoreboards.isEmpty()) {
                                return;
                            }

                            // 防止索引越界
                            if (index[0] >= scoreboards.size()) {
                                index[0] = 0;
                            }

                            // 获取当前要显示的计分板
                            ScoreboardItem currentItem = scoreboards.get(index[0]);
                            ScoreboardObjective objective = scoreboard.getNullableObjective(currentItem.getInternalName());

                            // 切换显示
                            if (objective != null) {
                                scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
                            }

                            // 索引 +1，下次切换下一个
                            index[0]++;
                        });
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage());
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
        });
    }

    // 同步数据至游戏计分板
    public static void syncData(MinecraftServer server) {
        server.submit(() -> {
            new Thread(() -> {
                while (!server.isStopped()) {
                    try {
                        Thread.sleep(Global.config.getSaveInterval()); // 根据周期执行
                        server.execute(() -> {
                            try {
                                // 遍历所有计分板，将配置数据同步到游戏内计分板
                                syncDataToScoreboard(server);
                                logger.trace("scoreboard updated");

                                // 将数据保存至本地
                                Global.config.saveConfig(); // 保存到本地文件
                                logger.trace("config updated");

                            } catch (Exception e) {
                                logger.error("config sync failed: {}", e.getMessage());
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

    // 数据同步逻辑
    private static void syncDataToScoreboard(MinecraftServer server) {
        // 遍历所有配置的计分板
        for (ScoreboardItem item : Global.config.getScoreboards()) {
            String internalName = item.getInternalName();
            ScoreboardObjective objective = scoreboard.getNullableObjective(internalName);
            if (objective == null) continue;
            switch (internalName) {
                case Config.ONLINE_TIME_INTERNAL_NAME -> {
                    // 遍历所有在线玩家，更新在线时长
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        // 获取内置统计：游玩时间-tick
                        int totalPlayTicks = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
                        if (totalPlayTicks == 0) continue;
                        int totalHours = totalPlayTicks / 20 / 3600;
                        // 更新配置对象
                        String playerName = player.getName().getString();
                        item.updateData(playerName, totalHours);
                    }
                }
                case Config.ELYTRON_DISTANCE_INTERNAL_NAME -> {
                    // 遍历所有在线玩家，更新飞行距离
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        // 获取内置统计：飞行距离
                        int aviateOneCM = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.AVIATE_ONE_CM)); // 单位cm
                        if (aviateOneCM == 0) continue;
                        int aviateOneKM = aviateOneCM / 100 / 1000; // 转化为km
                        // 更新配置对象
                        String playerName = player.getName().getString();
                        item.updateData(playerName, aviateOneKM);
                    }
                }
                case Config.DAMAGE_TAKEN_INTERNAL_NAME -> {
                    // 遍历所有玩家，更新受到的伤害
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        // 获取内置统计-半心
                        int damageTaken = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN)) / 10;
                        if (damageTaken == 0) continue;
                        // 更新配置对象
                        String playerName = player.getName().getString();
                        item.updateData(playerName, damageTaken);
                    }
                }
            }

            // 将配置中所有玩家数据（包括离线玩家）同步到游戏计分板
            for (Map.Entry<String, Integer> entry : item.getData().entrySet()) {
                String playerName = entry.getKey();
                int scoreValue = entry.getValue();

                ScoreHolder scoreHolder = ScoreHolder.fromName(playerName);
                ScoreAccess scoreAccess = scoreboard.getOrCreateScore(scoreHolder, objective);
                scoreAccess.setScore(scoreValue);
            }
        }
    }

}