// EventListeners.java
package cn.hycer.advancedscoreboard;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class EventListeners {

    public static void register() {
        // 服务器tick事件 - 用于更新计分板
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (AdvancedScoreboard.scoreboardManager != null) {
                AdvancedScoreboard.scoreboardManager.updateScoreboard();
            }
        });

        // 玩家死亡事件
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (AdvancedScoreboard.statsManager != null) {
                AdvancedScoreboard.statsManager.onPlayerDeath(newPlayer);
            }
        });

        // 玩家从死亡返回事件（更好的死亡统计）
//        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
//            // 检查玩家是否刚刚死亡
//            server.get
//            if (handler.player.getLastDeathLocation().isPresent()) {
//                // 可以在这里添加死亡统计逻辑
//            }
//        });
    }
}