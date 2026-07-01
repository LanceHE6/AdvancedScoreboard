package cn.hycer.advancedscoreboard.Global;

import cn.hycer.advancedscoreboard.Config.Config;
import net.minecraft.world.scores.Scoreboard;
import org.apache.logging.log4j.Logger;

public class Global {
    // 全局logger
    public static Logger logger;
    // 计分板对象
    public static Scoreboard scoreboard;
    // 全局配置实例
    public static Config config;
}
