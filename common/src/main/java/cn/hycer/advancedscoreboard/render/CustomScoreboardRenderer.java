package cn.hycer.advancedscoreboard.render;

import static cn.hycer.advancedscoreboard.Global.Global.logger;
import static cn.hycer.advancedscoreboard.Global.Global.scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class CustomScoreboardRenderer {

    private static final String PREFIX = "asb_v_";
    private static final Map<UUID, List<String>> playerScoreHolders = new HashMap<>();

    public static String getObjectiveId(ServerPlayer player) {
        return PREFIX + player.getUUID().toString().replace("-", "");
    }

    private static Objective ensureObjective(ServerPlayer player) {
        String objName = getObjectiveId(player);
        Objective obj = scoreboard.getObjective(objName);
        if (obj == null) {
            obj = scoreboard.addObjective(
                objName,
                ObjectiveCriteria.DUMMY,
                Component.empty(),
                ObjectiveCriteria.RenderType.INTEGER,
                true,
                null
            );
        }
        return obj;
    }

    public static void sendDisplay(ServerPlayer player, ScoreboardItem item) {
        try {
            Objective obj = ensureObjective(player);
            UUID uuid = player.getUUID();

            obj.setDisplayName(Component.literal(Global.config.getFormattedDisplayName(item)));
            player.connection.send(
                new ClientboundSetObjectivePacket(obj, ClientboundSetObjectivePacket.METHOD_ADD)
            );

            List<String> oldHolders = playerScoreHolders.getOrDefault(uuid, List.of());
            for (String holderName : oldHolders) {
                player.connection.send(
                    new ClientboundResetScorePacket(holderName, obj.getName())
                );
            }

            List<Map.Entry<String, Integer>> sorted = item.getData().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(Global.config.getMaxDisplayNum())
                .toList();

            List<String> newHolders = new ArrayList<>();
            for (int i = 0; i < sorted.size(); i++) {
                Map.Entry<String, Integer> entry = sorted.get(i);
                int rank = i + 1;
                String holderName = "§" + String.format("%02x", rank) + entry.getKey();
                Component display = Component.literal("§e#" + rank + " §f" + entry.getKey() + " §7- §a" + entry.getValue());

                newHolders.add(holderName);
                player.connection.send(
                    new ClientboundSetScorePacket(holderName, obj.getName(), entry.getValue(), Optional.of(display), Optional.empty())
                );
            }
            playerScoreHolders.put(uuid, newHolders);

            player.connection.send(
                new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, obj)
            );
        } catch (Exception e) {
            logger.error("failed to send custom display to player {}: {}", player.getScoreboardName(), e.getMessage(), e);
        }
    }

    public static void clearDisplay(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Objective obj = scoreboard.getObjective(getObjectiveId(player));
        if (obj != null) {
            List<String> holders = playerScoreHolders.getOrDefault(uuid, List.of());
            for (String holderName : holders) {
                player.connection.send(
                    new ClientboundResetScorePacket(holderName, obj.getName())
                );
            }
            player.connection.send(
                new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, (Objective) null)
            );
        }
        playerScoreHolders.remove(uuid);
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        UUID uuid = player.getUUID();
        String objName = getObjectiveId(player);
        Objective obj = scoreboard.getObjective(objName);
        if (obj != null) {
            scoreboard.removeObjective(obj);
        }
        playerScoreHolders.remove(uuid);
        cn.hycer.advancedscoreboard.Task.Task.removePlayer(uuid);
    }
}
