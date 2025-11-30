// ScoreboardManager.java
package cn.hycer.advancedscoreboard;

import net.minecraft.component.Component;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;


import java.text.NumberFormat;
import java.util.*;

public class ScoreboardManager {
    private final Map<UUID, Set<StatType>> playerVisibleStats = new HashMap<>();
    private final Map<UUID, Integer> playerCurrentDisplayIndex = new HashMap<>();
    private final Map<UUID, Long> playerLastSwitchTime = new HashMap<>();

    private static final int SWITCH_INTERVAL = 5000; // 5秒
    private Scoreboard serverScoreboard;

    public void onPlayerJoin(ServerPlayerEntity player) {
        serverScoreboard = AdvancedScoreboard.configManager.getServer().getScoreboard();
        setupPlayerScoreboard(player);
    }

    public void onPlayerLeave(ServerPlayerEntity player) {
        // 清理资源
        playerVisibleStats.remove(player.getUuid());
        playerCurrentDisplayIndex.remove(player.getUuid());
        playerLastSwitchTime.remove(player.getUuid());
    }

    private void setupPlayerScoreboard(ServerPlayerEntity player) {
        // 设置默认可见的统计项
        Set<StatType> visibleStats = new LinkedHashSet<>(Arrays.asList(StatType.values()));
        playerVisibleStats.put(player.getUuid(), visibleStats);
        playerCurrentDisplayIndex.put(player.getUuid(), 0);
        playerLastSwitchTime.put(player.getUuid(), System.currentTimeMillis());
    }

    public void updateScoreboard() {
        MinecraftServer server = AdvancedScoreboard.configManager.getServer();
        if (server == null) return;

        long currentTime = System.currentTimeMillis();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID uuid = player.getUuid();
            Set<StatType> visibleStats = playerVisibleStats.get(uuid);

            if (visibleStats == null || visibleStats.isEmpty()) {
                hideScoreboard(player);
                continue;
            }

            // 检查是否需要切换显示
            if (currentTime - playerLastSwitchTime.getOrDefault(uuid, 0L) >= SWITCH_INTERVAL) {
                switchToNextStat(player, visibleStats);
                playerLastSwitchTime.put(uuid, currentTime);
            }

            updateDisplay(player, getCurrentStat(player, visibleStats));
        }
    }

    private void switchToNextStat(ServerPlayerEntity player, Set<StatType> visibleStats) {
        int currentIndex = playerCurrentDisplayIndex.getOrDefault(player.getUuid(), 0);
        List<StatType> statsList = new ArrayList<>(visibleStats);

        if (statsList.isEmpty()) return;

        int nextIndex = (currentIndex + 1) % statsList.size();
        playerCurrentDisplayIndex.put(player.getUuid(), nextIndex);
    }

    private StatType getCurrentStat(ServerPlayerEntity player, Set<StatType> visibleStats) {
        int currentIndex = playerCurrentDisplayIndex.getOrDefault(player.getUuid(), 0);
        List<StatType> statsList = new ArrayList<>(visibleStats);

        if (statsList.isEmpty()) return null;

        return statsList.get(currentIndex % statsList.size());
    }

    private void updateDisplay(ServerPlayerEntity player, StatType currentStat) {
        if (currentStat == null) {
            hideScoreboard(player);
            return;
        }
        ScoreboardObjective objective = null;
        for (ScoreboardObjective obj : serverScoreboard.getObjectives()) {
            if (obj.getName().equals("advanced_scoreboard")) {
                objective = obj;
                break;
            }
        }

        if (objective == null) {
            objective = serverScoreboard.addObjective("advanced_scoreboard",
                    ScoreboardCriterion.DUMMY,
                    Text.literal("高级统计"),
                    ScoreboardCriterion.RenderType.INTEGER,
                    true, BlankNumberFormat.INSTANCE);
        }

        serverScoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);


// 清除旧分数
        for (String entry : serverScoreboard.getObjectiveNames()) {
            serverScoreboard.resetPlayerScore(entry, objective);
        }

        // 设置标题
        objective.setDisplayName(Component.literal(currentStat.getFormattedName()));

        // 获取并显示前10名玩家
        List<Map.Entry<UUID, PlayerStats>> topPlayers = getTopPlayers(currentStat, 10);

        for (int i = 0; i < topPlayers.size(); i++) {
            Map.Entry<UUID, PlayerStats> entry = topPlayers.get(i);
            PlayerStats stats = entry.getValue();
            String displayName = stats.getPlayerName();
            String value = stats.getFormattedStat(currentStat);

            // 创建显示文本
            String scoreText = String.format("§e%d. §f%s: §b%s", i + 1, displayName, value);
            objective.getScore(scoreText).setScore(topPlayers.size() - i);
        }
    }

    private void hideScoreboard(ServerPlayerEntity player) {
        Objective objective = serverScoreboard.getObjective("advanced_scoreboard");
        if (objective != null) {
            serverScoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
        }
    }

    private List<Map.Entry<UUID, PlayerStats>> getTopPlayers(StatType statType, int count) {
        Map<UUID, PlayerStats> allStats = AdvancedScoreboard.statsManager.getAllStats();
        List<Map.Entry<UUID, PlayerStats>> sortedList = new ArrayList<>(allStats.entrySet());

        sortedList.sort((a, b) -> {
            PlayerStats statsA = a.getValue();
            PlayerStats statsB = b.getValue();

            switch (statType) {
                case BLOCKS_MINED:
                    return Long.compare(statsB.getBlocksMined(), statsA.getBlocksMined());
                case DEATHS:
                    return Integer.compare(statsB.getDeaths(), statsA.getDeaths());
                case ONLINE_TIME:
                    return Long.compare(statsB.getOnlineTime(), statsA.getOnlineTime());
                case ELYTRA_DISTANCE:
                    return Double.compare(statsB.getElytraDistance(), statsA.getElytraDistance());
                default:
                    return 0;
            }
        });

        return sortedList.subList(0, Math.min(count, sortedList.size()));
    }

    public void setPlayerVisibleStats(UUID playerUUID, Set<StatType> stats) {
        playerVisibleStats.put(playerUUID, new LinkedHashSet<>(stats));
        playerCurrentDisplayIndex.put(playerUUID, 0);
        playerLastSwitchTime.put(playerUUID, System.currentTimeMillis());
    }

    public Set<StatType> getPlayerVisibleStats(UUID playerUUID) {
        return playerVisibleStats.getOrDefault(playerUUID, new HashSet<>());
    }
}