// AdvancedScoreboardMod.java (部分修正)
package cn.hycer.advancedscoreboard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedScoreboard implements ModInitializer {
    public static final String MOD_ID = "advancedscoreboard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static PlayerStatsManager statsManager;
    public static ScoreboardManager scoreboardManager;
    public static ConfigManager configManager;

    @Override
    public void onInitialize() {
        LOGGER.info("AdvancedScoreboard Mod Initializing...");

        // 初始化管理器
        configManager = new ConfigManager();
        statsManager = new PlayerStatsManager();
        scoreboardManager = new ScoreboardManager();

        // 注册服务器启动事件
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            configManager.setServer(server);
        });

        // 注册事件
        registerEvents();

        // 注册命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ScoreboardCommand.register(dispatcher);
        });

        // 注册tick事件
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            EventListeners.register();
        });

        LOGGER.info("AdvancedScoreboard Mod Initialized!");
    }

    private void registerEvents() {
        // 玩家加入/退出事件
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            statsManager.onPlayerJoin(handler.player);
            scoreboardManager.onPlayerJoin(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            statsManager.onPlayerLeave(handler.player);
            scoreboardManager.onPlayerLeave(handler.player);
        });

        // 方块破坏事件
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            statsManager.onBlockBreak((ServerPlayerEntity) player);
        });

        // 服务器停止事件
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            configManager.saveAllStats();
        });
    }
}