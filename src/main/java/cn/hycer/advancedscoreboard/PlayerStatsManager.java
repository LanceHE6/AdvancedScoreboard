// PlayerStatsManager.java
package cn.hycer.advancedscoreboard;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatsManager {
    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();

    public void onPlayerJoin(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        PlayerStats stats = playerStats.computeIfAbsent(uuid,
                k -> new PlayerStats(uuid, player.getNameForScoreboard()));
        stats.setJoinTime();

        // 从配置文件加载数据
        AdvancedScoreboard.configManager.loadPlayerStats(stats);
    }

    public void onPlayerLeave(ServerPlayerEntity player) {
        PlayerStats stats = playerStats.get(player.getUuid());
        if (stats != null) {
            stats.updateOnlineTime();
            // 保存到配置文件
            AdvancedScoreboard.configManager.savePlayerStats(stats);
        }
    }

    public void onBlockBreak(ServerPlayerEntity player) {
        PlayerStats stats = playerStats.get(player.getUuid());
        if (stats != null) {
            stats.incrementBlocksMined();
        }
    }

    public void onPlayerDeath(ServerPlayerEntity player) {
        PlayerStats stats = playerStats.get(player.getUuid());
        if (stats != null) {
            stats.incrementDeaths();
        }
    }

    public void onElytraFlight(ServerPlayerEntity player, double distance) {
        PlayerStats stats = playerStats.get(player.getUuid());
        if (stats != null) {
            stats.addElytraDistance(distance);
        }
    }

    public PlayerStats getPlayerStats(UUID uuid) {
        return playerStats.get(uuid);
    }

    public Map<UUID, PlayerStats> getAllStats() {
        return new HashMap<>(playerStats);
    }
}