package cn.hycer.advancedscoreboard;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Event.PlayerBreakBlockEvent;
import cn.hycer.advancedscoreboard.Event.ServerStartedEvent;
import cn.hycer.advancedscoreboard.Global.Global;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdvancedScoreboard implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    public Config config;

    @Override
    public void onInitialize() {

        try {
            config = new Config(FabricLoader.getInstance().getConfigDir().toFile().getPath());
            // 将配置实例设置到全局
            Global.config = this.config;
        } catch (Exception e) {
            LOGGER.error("Could not load config file,mod did not load,err: ", e.getMessage());
            return;
        }
        //注册服务器启动完毕的事件
        ServerLifecycleEvents.SERVER_STARTED.register(ServerStartedEvent::onServerStarted);

        //注册玩家破坏方块的事件
        PlayerBlockBreakEvents.AFTER.register(((world, playerEntity, blockPos, blockState, blockEntity) ->
                PlayerBreakBlockEvent.onBreak(playerEntity)));

        System.out.println("[AdvancedScoreboard] mod load success！");
    }
}