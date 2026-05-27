package cn.hycer.advancedscoreboard.Event;

import static cn.hycer.advancedscoreboard.Global.Global.logger;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.ActionResult;

public class PlayerPlaceBlockEvent {

    // 从配置文件中获取计分板名称
    private static final String internalName = Config.PLACE_COUNT_INTERNAL_NAME;

    public static ActionResult onPlace(PlayerEntity player) {

        ScoreboardItem placeCountScoreboard = Global.config.getScoreboardByInternalName(internalName);
        //获取玩家的分数
        String playerName = player.getName().getString();
        int playerScore = placeCountScoreboard.getDataValue(playerName, 0);
        //更新玩家的分数
        Global.config.getScoreboardByInternalName(Config.PLACE_COUNT_INTERNAL_NAME).updateData(playerName, ++playerScore);

        // 同步到游戏内计分板
        syncToScoreboard(playerName, playerScore);

        logger.debug(Global.config.toString());
        return ActionResult.PASS;
    }

    /**
     * 同步玩家放置数据到游戏内计分板
     */
    private static void syncToScoreboard(String playerName, int playerScore) {
        try {
            // 获取计分板对象
            ScoreboardObjective objective = Global.scoreboard.getNullableObjective(internalName);
            if (objective != null) {
                // 更新游戏内计分板
                ScoreHolder scoreHolder = ScoreHolder.fromName(playerName);
                ScoreAccess scoreAccess = Global.scoreboard.getOrCreateScore(scoreHolder, objective);
                scoreAccess.setScore(playerScore);
            }
        } catch (Exception e) {
            logger.error("Failed to sync player data to scoreboard: {}", e.getMessage());
        }
    }
}
