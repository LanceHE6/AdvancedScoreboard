package cn.hycer.advancedscoreboard;

import cn.hycer.advancedscoreboard.Command.ASBCommand;
import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Event.PlayerBreakBlockEvent;
import cn.hycer.advancedscoreboard.Event.PlayerKillMobEvent;
import cn.hycer.advancedscoreboard.Event.ServerStartedEvent;
import cn.hycer.advancedscoreboard.Global.Global;
import static cn.hycer.advancedscoreboard.Global.Global.logger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;

public class AdvancedScoreboard implements ModInitializer {

    @Override
    public void onInitialize() {
        // 初始化全局日志
        logger = LogManager.getLogger();
        try {
            // 初始化全局配置
            Global.config = new Config(FabricLoader.getInstance().getConfigDir().toFile().getPath());
        } catch (Exception e) {
            logger.error("could not load config file, mod did not load, err: {}", e.getMessage());
            return;
        }

        //注册指令
        ASBCommand.register();

        //注册服务器启动完毕的事件
        ServerLifecycleEvents.SERVER_STARTED.register(ServerStartedEvent::onServerStarted);

        //注册玩家破坏方块的事件
        PlayerBlockBreakEvents.AFTER.register(((world, playerEntity, blockPos, blockState, blockEntity) ->
                PlayerBreakBlockEvent.onBreak(playerEntity)));

        //注册玩家击杀生物的事件
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(
            (world, entity, killedEntity, damageSource) ->
                PlayerKillMobEvent.onKill(world, entity, killedEntity)
        );

        logger.info("advancedScoreboard load success!");
    }
}
