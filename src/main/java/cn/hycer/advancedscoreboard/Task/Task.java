package cn.hycer.advancedscoreboard.Task;

import static cn.hycer.advancedscoreboard.Global.Global.logger;
import static cn.hycer.advancedscoreboard.Global.Global.scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;

public class Task {

    // 每位玩家的当前轮播索引
    private static final Map<String, Integer> playerRotationIndex = new HashMap<>();

    // 轮播切换任务
    public static void scoreboardSwitch(MinecraftServer server) {
        server.submit(() -> {
            new Thread(() -> {
                while (!server.isStopped()) {
                    try {
                        // 每次循环都从配置读取间隔，支持热修改
                        int intervalSeconds = Global.config.getSwitchInterval();
                        long intervalMs = (long) intervalSeconds * 1000;
                        Thread.sleep(intervalMs);

                        server.execute(() -> {
                            List<ScoreboardItem> allScoreboards = Global.config.getScoreboards();
                            if (allScoreboards == null || allScoreboards.isEmpty()) {
                                return;
                            }

                            // 为每位在线玩家单独发送计分板显示包
                            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                                String playerName = player.getName().getString();

                                // 过滤该玩家隐藏的榜单
                                Set<String> hidden = Global.config.getHiddenScoreboards(playerName);
                                List<ScoreboardItem> visibleScoreboards = allScoreboards.stream()
                                    .filter(sb -> !hidden.contains(sb.getInternalName()))
                                    .toList();

                                if (visibleScoreboards.isEmpty()) {
                                    continue;
                                }

                                // 推进该玩家的轮播索引
                                int index = playerRotationIndex.getOrDefault(playerName, -1);
                                index = (index + 1) % visibleScoreboards.size();
                                playerRotationIndex.put(playerName, index);

                                // 向该玩家单独发送显示包
                                ScoreboardItem currentItem = visibleScoreboards.get(index);
                                ScoreboardObjective objective = scoreboard.getNullableObjective(currentItem.getInternalName());
                                if (objective != null) {
                                    player.networkHandler.sendPacket(
                                        new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, objective)
                                    );
                                }
                            }
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

            // 按 maxDisplayNum 限制同步玩家数据到游戏计分板
            syncTopNToScoreboard(objective, item.getData(), Global.config.getMaxDisplayNum());
        }
    }

    /**
     * 将数据按分数降序排列后，仅同步前 maxDisplay 名玩家到游戏内计分板
     * 同时移除不在前 N 名的玩家条目
     */
    public static void syncTopNToScoreboard(ScoreboardObjective objective, Map<String, Integer> data, int maxDisplay) {
        // 按分数降序排序，取前 maxDisplay 名
        List<Map.Entry<String, Integer>> topEntries = data.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(maxDisplay)
                .toList();

        Set<String> topPlayerNames = topEntries.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // 移除不在前 N 名的玩家条目
        for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
            if (!topPlayerNames.contains(entry.owner())) {
                scoreboard.removeScore(ScoreHolder.fromName(entry.owner()), objective);
            }
        }

        // 添加 / 更新前 N 名玩家的分数
        for (Map.Entry<String, Integer> entry : topEntries) {
            ScoreHolder scoreHolder = ScoreHolder.fromName(entry.getKey());
            ScoreAccess scoreAccess = scoreboard.getOrCreateScore(scoreHolder, objective);
            scoreAccess.setScore(entry.getValue());
        }
    }

}