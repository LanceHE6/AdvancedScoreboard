package cn.hycer.advancedscoreboard.Event;

import static cn.hycer.advancedscoreboard.Global.Global.logger;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import cn.hycer.advancedscoreboard.Task.Task;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.ActionResult;

public class PlayerPlaceBlockEvent {

    private static final String internalName = Config.PLACE_COUNT_INTERNAL_NAME;

    public static ActionResult onPlace(PlayerEntity player) {
        ScoreboardItem item = Global.config.getScoreboardByInternalName(internalName);
        String playerName = player.getName().getString();
        int playerScore = item.getDataValue(playerName, 0);
        item.updateData(playerName, ++playerScore);

        syncToScoreboard(playerName, playerScore);

        Task.refreshDisplayForItem(player.getEntityWorld().getServer(), item);

        logger.debug(Global.config.toString());
        return ActionResult.PASS;
    }

    private static void syncToScoreboard(String playerName, int playerScore) {
        try {
            ScoreboardObjective objective = Global.scoreboard.getNullableObjective(internalName);
            if (objective != null) {
                ScoreHolder scoreHolder = ScoreHolder.fromName(playerName);
                ScoreAccess scoreAccess = Global.scoreboard.getOrCreateScore(scoreHolder, objective);
                scoreAccess.setScore(playerScore);
            }
        } catch (Exception e) {
            logger.error("Failed to sync player data to scoreboard: {}", e.getMessage());
        }
    }
}
