package cn.hycer.advancedscoreboard.Event;

import static cn.hycer.advancedscoreboard.Global.Global.logger;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import net.minecraft.entity.player.PlayerEntity;

public class PlayerBreakBlockEvent {

    // 从配置文件中获取计分板名称
    private static final String internalName = Config.MINE_COUNT_INTERNAL_NAME;

    public static void onBreak(PlayerEntity player) {

        ScoreboardItem mineCountScoreboard = Global.config.getScoreboardByInternalName(internalName);
        //获取玩家的分数
        String playerName = player.getName().getString();
        int playerScore = mineCountScoreboard.getDataValue(playerName, 0);
        //更新玩家的分数
        Global.config.getScoreboardByInternalName(Config.MINE_COUNT_INTERNAL_NAME).updateData(playerName, ++playerScore);
        logger.debug(Global.config.toString());
    }
}