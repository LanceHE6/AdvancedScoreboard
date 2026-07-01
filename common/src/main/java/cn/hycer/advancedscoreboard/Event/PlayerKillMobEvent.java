package cn.hycer.advancedscoreboard.Event;

import static cn.hycer.advancedscoreboard.Global.Global.logger;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import cn.hycer.advancedscoreboard.Task.Task;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class PlayerKillMobEvent {

    private static final String internalName = Config.MOB_KILLS_INTERNAL_NAME;

    public static void onKill(net.minecraft.server.level.ServerLevel world, Entity entity, LivingEntity killedEntity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (killedEntity instanceof Player) {
            return;
        }

        ScoreboardItem item = Global.config.getScoreboardByInternalName(internalName);
        String playerName = player.getScoreboardName();
        int playerScore = item.getDataValue(playerName, 0);
        item.updateData(playerName, ++playerScore);

        ScoreboardItem dataItem = Global.config.getScoreboardByInternalName(internalName);
        if (dataItem != null) {
            Task.refreshDisplayForItem(world.getServer(), dataItem);
        }

        logger.debug("kill - {}: {}", playerName, playerScore);
    }
}
