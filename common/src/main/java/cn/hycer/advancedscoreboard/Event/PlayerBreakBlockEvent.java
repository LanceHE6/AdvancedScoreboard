package cn.hycer.advancedscoreboard.Event;

import static cn.hycer.advancedscoreboard.Global.Global.logger;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import cn.hycer.advancedscoreboard.Task.Task;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public class PlayerBreakBlockEvent {

    private static final String internalName = Config.MINE_COUNT_INTERNAL_NAME;

    public static void onBreak(Player player) {
        ScoreboardItem item = Global.config.getScoreboardByInternalName(internalName);
        String playerName = player.getScoreboardName();
        int playerScore = item.getDataValue(playerName, 0);
        item.updateData(playerName, ++playerScore);

        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        ScoreboardItem dataItem = Global.config.getScoreboardByInternalName(internalName);
        if (server != null && dataItem != null) {
            Task.refreshDisplayForItem(server, dataItem);
        }

        logger.debug("break - {}: {}", playerName, playerScore);
    }
}
