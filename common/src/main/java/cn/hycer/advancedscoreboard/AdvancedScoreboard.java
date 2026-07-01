package cn.hycer.advancedscoreboard;

import cn.hycer.advancedscoreboard.Command.ASBCommand;
import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Event.PlayerBreakBlockEvent;
import cn.hycer.advancedscoreboard.Event.PlayerKillMobEvent;
import cn.hycer.advancedscoreboard.Event.PlayerPlaceBlockEvent;
import cn.hycer.advancedscoreboard.Event.ServerStartedEvent;
import cn.hycer.advancedscoreboard.Global.Global;
import static cn.hycer.advancedscoreboard.Global.Global.logger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import org.apache.logging.log4j.LogManager;

public class AdvancedScoreboard implements ModInitializer {

    @Override
    public void onInitialize() {
        logger = LogManager.getLogger();
        try {
            Global.config = new Config(FabricLoader.getInstance().getConfigDir().toFile().getPath());
        } catch (Exception e) {
            logger.error("could not load config file, mod did not load, err: {}", e.getMessage());
            return;
        }

        ASBCommand.register();

        ServerLifecycleEvents.SERVER_STARTED.register(ServerStartedEvent::onServerStarted);

        PlayerBlockBreakEvents.AFTER.register(((world, playerEntity, blockPos, blockState, blockEntity) ->
                PlayerBreakBlockEvent.onBreak(playerEntity)));

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player.getItemInHand(hand).getItem() instanceof BlockItem) {
                PlayerPlaceBlockEvent.onPlace(player);
            }
            return InteractionResult.PASS;
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(
            (world, entity, killedEntity, damageSource) ->
                PlayerKillMobEvent.onKill(world, entity, killedEntity)
        );

        logger.info("advancedScoreboard load success!");
    }
}
