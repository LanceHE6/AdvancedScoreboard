package cn.hycer.advancedscoreboard.Event;

import static cn.hycer.advancedscoreboard.Global.Global.logger;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;

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

        syncToScoreboard(playerName, playerScore);

        logger.debug(Global.config.toString());
    }

    private static void syncToScoreboard(String playerName, int playerScore) {
        try {
            Objective objective = Global.scoreboard.getObjective(internalName);
            if (objective != null) {
                ScoreHolder scoreHolder = ScoreHolder.forNameOnly(playerName);
                ScoreAccess scoreAccess = Global.scoreboard.getOrCreatePlayerScore(scoreHolder, objective);
                scoreAccess.set(playerScore);
            }
        } catch (Exception e) {
            logger.error("Failed to sync player data to scoreboard: {}", e.getMessage());
        }
    }
}
