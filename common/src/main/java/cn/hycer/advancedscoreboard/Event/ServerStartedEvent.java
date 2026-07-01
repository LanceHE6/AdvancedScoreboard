package cn.hycer.advancedscoreboard.Event;

import cn.hycer.advancedscoreboard.Config.Config;
import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import cn.hycer.advancedscoreboard.Task.Task;
import cn.hycer.advancedscoreboard.render.CustomScoreboardRenderer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.ScoreHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.hycer.advancedscoreboard.Global.Global.logger;
import static cn.hycer.advancedscoreboard.Global.Global.scoreboard;

public class ServerStartedEvent {

    public static void onServerStarted(MinecraftServer server) {
        scoreboard = Objects.requireNonNull(server.overworld().getScoreboard());
        clearInGameScoreboardData(server);
        registerScoreboard(server);
        setLatencyToListDisplay();
        syncDataFromConfig();
        ServerTickEvents.END_SERVER_TICK.register(Task::onServerTick);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server1) -> {
            if (handler.player != null) {
                CustomScoreboardRenderer.onPlayerDisconnect(handler.player);
            }
        });
    }

    public static void registerScoreboard(MinecraftServer server) {
        List<ScoreboardItem> scoreboards = Global.config.getScoreboards();
        for (ScoreboardItem sb : scoreboards) {
            try {
                String formattedName = Global.config.getFormattedDisplayName(sb);
                Objective scoreboardObj = scoreboard.getObjective(sb.getInternalName());
                if (scoreboardObj != null) {
                    continue;
                }
                scoreboardObj = scoreboard.addObjective(
                    sb.getInternalName(),
                    net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,
                    net.minecraft.network.chat.Component.literal(formattedName),
                    net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER,
                    true,
                    null
                );
                if (scoreboardObj != null) {
                    logger.info("registered scoreboard: {}", sb.getInternalName());
                } else {
                    logger.error("failed to register scoreboard: {} (addObjective returned null)", sb.getInternalName());
                }
            } catch (Exception e) {
                logger.error("failed to register scoreboard: {}, reason: {}", sb.getInternalName(), e.getMessage());
            }
        }
    }

    private static void setLatencyToListDisplay() {
        if (scoreboard == null) return;
        Objective latencyObj = scoreboard.getObjective(Config.LATENCY_INTERNAL_NAME);
        if (latencyObj != null) {
            scoreboard.setDisplayObjective(net.minecraft.world.scores.DisplaySlot.LIST, latencyObj);
            logger.info("latency objective set to LIST display slot");
        }
    }

    public static void refreshAllDisplayNames() {
        if (scoreboard == null) return;
        logger.info("all scoreboard display names refreshed");
    }

    public static void syncDataFromConfig() {
        try {
            for (ScoreboardItem item : Global.config.getScoreboards()) {
                String internalName = item.getInternalName();
                Objective objective = scoreboard.getObjective(internalName);
                if (objective == null) continue;

                Task.syncTopNToScoreboard(objective, item.getData(), Global.config.getMaxDisplayNum());
            }
            logger.info("synced all player data from config to scoreboard");
        } catch (Exception e) {
            logger.error("sync data from config failed: {}", e.getMessage());
        }
    }

    public static void clearInGameScoreboardData(MinecraftServer server) {
        if (scoreboard == null) {
            scoreboard = Objects.requireNonNull(server.overworld().getScoreboard());
        }

        Set<String> currentInternalNames = Global.config.getScoreboards().stream()
                .map(ScoreboardItem::getInternalName)
                .collect(Collectors.toSet());
        Set<String> obsoleteNames = Set.of("elytron_distance", "latency");

        try {
            List<Objective> toRemove = new ArrayList<>();
            for (Objective objective : scoreboard.getObjectives()) {
                String name = objective.getName();
                if (obsoleteNames.contains(name)) {
                    toRemove.add(objective);
                } else if (currentInternalNames.contains(name)) {
                    for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
                        scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly(entry.owner()), objective);
                    }
                }
            }

            for (Objective obj : toRemove) {
                scoreboard.removeObjective(obj);
                logger.info("removed obsolete objective: {}", obj.getName());
            }

            logger.info("scoreboard data cleared, removed {} obsolete objectives", toRemove.size());
        } catch (Exception e) {
            logger.error("reset scoreboard data failed: {}", e.getMessage());
        }
    }
}
