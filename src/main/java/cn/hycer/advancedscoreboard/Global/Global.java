package cn.hycer.advancedscoreboard.Global;

import cn.hycer.advancedscoreboard.Config.Config;
import net.minecraft.scoreboard.Scoreboard;

public class Global {
    // 计分板对象
    public static Scoreboard scoreboard;
    // 全局配置实例
    public static Config config;
    // 当前显示的计分板类型
    public static boolean isShowingMining = true;
}
