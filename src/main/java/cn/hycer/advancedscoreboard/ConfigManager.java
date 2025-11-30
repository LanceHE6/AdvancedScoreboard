// ConfigManager.java
package cn.hycer.advancedscoreboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private Path configDir;
    private Map<String, Boolean> enabledStats = new HashMap<>();
    private Map<String, String> customTitles = new HashMap<>();
    private MinecraftServer server;

    public void setServer(MinecraftServer server) {
        this.server = server;
        loadConfig();
    }

    public void loadConfig() {
        try {
            // 使用FabricLoader获取配置目录
            configDir = FabricLoader.getInstance().getConfigDir().resolve("advancedscoreboard");
            Files.createDirectories(configDir);

            // 加载统计项启用状态
            loadEnabledStats();

            // 加载自定义标题
            loadCustomTitles();

            AdvancedScoreboard.LOGGER.info("Config loaded from: " + configDir.toString());

        } catch (IOException e) {
            AdvancedScoreboard.LOGGER.error("Failed to load config", e);
        }
    }

    public void loadPlayerStats(PlayerStats stats) {
        try {
            Path playerFile = configDir.resolve("playerdata").resolve(stats.getPlayerUUID().toString() + ".json");
            if (Files.exists(playerFile)) {
                String json = Files.readString(playerFile);
                @SuppressWarnings("unchecked")
                Map<String, Object> data = GSON.fromJson(json, Map.class);
                stats.fromMap(data);
                AdvancedScoreboard.LOGGER.debug("Loaded stats for player: " + stats.getPlayerName());
            }
        } catch (Exception e) {
            AdvancedScoreboard.LOGGER.error("Failed to load player stats for " + stats.getPlayerName(), e);
        }
    }

    public void savePlayerStats(PlayerStats stats) {
        try {
            Path playerDir = configDir.resolve("playerdata");
            Files.createDirectories(playerDir);

            Path playerFile = playerDir.resolve(stats.getPlayerUUID().toString() + ".json");
            String json = GSON.toJson(stats.toMap());
            Files.writeString(playerFile, json);
            AdvancedScoreboard.LOGGER.debug("Saved stats for player: " + stats.getPlayerName());
        } catch (Exception e) {
            AdvancedScoreboard.LOGGER.error("Failed to save player stats for " + stats.getPlayerName(), e);
        }
    }

    public void saveAllStats() {
        AdvancedScoreboard.LOGGER.info("Saving all player stats...");
        for (PlayerStats stats : AdvancedScoreboard.statsManager.getAllStats().values()) {
            stats.updateOnlineTime(); // 确保在线时间更新
            savePlayerStats(stats);
        }
        saveEnabledStats();
        saveCustomTitles();
        AdvancedScoreboard.LOGGER.info("All stats saved successfully");
    }

    private void loadEnabledStats() {
        try {
            Path file = configDir.resolve("enabled_stats.json");
            if (Files.exists(file)) {
                String json = Files.readString(file);
                @SuppressWarnings("unchecked")
                Map<String, Boolean> loadedStats = GSON.fromJson(json, Map.class);
                enabledStats = loadedStats;
                AdvancedScoreboard.LOGGER.info("Loaded enabled stats configuration");
            } else {
                // 默认所有统计项启用
                for (StatType type : StatType.values()) {
                    enabledStats.put(type.getId(), true);
                }
                saveEnabledStats(); // 创建默认配置文件
                AdvancedScoreboard.LOGGER.info("Created default enabled stats configuration");
            }
        } catch (Exception e) {
            AdvancedScoreboard.LOGGER.error("Failed to load enabled stats", e);
            // 出错时使用默认值
            for (StatType type : StatType.values()) {
                enabledStats.put(type.getId(), true);
            }
        }
    }

    private void saveEnabledStats() {
        try {
            Path file = configDir.resolve("enabled_stats.json");
            String json = GSON.toJson(enabledStats);
            Files.writeString(file, json);
            AdvancedScoreboard.LOGGER.debug("Saved enabled stats configuration");
        } catch (Exception e) {
            AdvancedScoreboard.LOGGER.error("Failed to save enabled stats", e);
        }
    }

    private void loadCustomTitles() {
        try {
            Path file = configDir.resolve("custom_titles.json");
            if (Files.exists(file)) {
                String json = Files.readString(file);
                @SuppressWarnings("unchecked")
                Map<String, String> loadedTitles = GSON.fromJson(json, Map.class);
                customTitles = loadedTitles;
                AdvancedScoreboard.LOGGER.info("Loaded custom titles configuration");
            }
        } catch (Exception e) {
            AdvancedScoreboard.LOGGER.error("Failed to load custom titles", e);
        }
    }

    private void saveCustomTitles() {
        try {
            Path file = configDir.resolve("custom_titles.json");
            String json = GSON.toJson(customTitles);
            Files.writeString(file, json);
            AdvancedScoreboard.LOGGER.debug("Saved custom titles configuration");
        } catch (Exception e) {
            AdvancedScoreboard.LOGGER.error("Failed to save custom titles", e);
        }
    }

    public MinecraftServer getServer() {
        return server;
    }

    public boolean isStatEnabled(StatType type) {
        return enabledStats.getOrDefault(type.getId(), true);
    }

    public void setStatEnabled(StatType type, boolean enabled) {
        enabledStats.put(type.getId(), enabled);
        saveEnabledStats();
    }

    public String getCustomTitle(StatType type) {
        return customTitles.getOrDefault(type.getId(), type.getFormattedName());
    }

    public void setCustomTitle(StatType type, String title) {
        customTitles.put(type.getId(), title);
        saveCustomTitles();
    }

    public Path getConfigDir() {
        return configDir;
    }
}