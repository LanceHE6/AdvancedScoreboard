package cn.hycer.advancedscoreboard.Event;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Global.Global;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreboardObjective;

public class PlayerBreakBlockEvent {
    public static void onBreak(PlayerEntity player) {
        String internalName = "mine_count";//从配置文件获取内部名字

        //获取记分对象
        ScoreboardObjective obj = Global.scoreboard.getNullableObjective(internalName);
        if (obj == null) return;

        ScoreAccess score = Global.scoreboard.getOrCreateScore(player, obj);

        //获取玩家的分数
        int playerScore = score.getScore();
        //更新玩家的分数
        score.setScore(++playerScore);
    }
}