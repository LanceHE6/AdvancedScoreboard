package cn.hycer.advancedscoreboard.render;

import static cn.hycer.advancedscoreboard.Global.Global.logger;
import static cn.hycer.advancedscoreboard.Global.Global.scoreboard;

import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreResetS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class CustomScoreboardRenderer {

    private static final String PREFIX = "asb_v_";

    // 每个玩家的虚拟 objective 中已发送的 score holder 名称列表
    private static final Map<UUID, List<String>> playerScoreHolders = new HashMap<>();

    public static String getObjectiveId(ServerPlayerEntity player) {
        return PREFIX + player.getUuid().toString().replace("-", "");
    }

    /**
     * 为玩家创建独立虚拟 objective
     */
    private static ScoreboardObjective ensureObjective(ServerPlayerEntity player) {
        String objName = getObjectiveId(player);
        ScoreboardObjective obj = scoreboard.getNullableObjective(objName);
        if (obj == null) {
            obj = scoreboard.addObjective(
                objName,
                ScoreboardCriterion.DUMMY,
                Text.empty(),
                ScoreboardCriterion.RenderType.INTEGER,
                true,
                null
            );
        }
        return obj;
    }

    /**
     * 向单个玩家发送自定义计分板显示
     */
    public static void sendDisplay(ServerPlayerEntity player, ScoreboardItem item) {
        try {
            ScoreboardObjective obj = ensureObjective(player);
            UUID uuid = player.getUuid();

            // 1. 更新 objective 显示名
            obj.setDisplayName(Text.literal(Global.config.getFormattedDisplayName(item)));
            player.networkHandler.sendPacket(
                new ScoreboardObjectiveUpdateS2CPacket(obj, ScoreboardObjectiveUpdateS2CPacket.UPDATE_MODE)
            );

            // 2. 清除旧条目（客户端 + 追踪列表）
            List<String> oldHolders = playerScoreHolders.getOrDefault(uuid, List.of());
            for (String holderName : oldHolders) {
                player.networkHandler.sendPacket(
                    new ScoreboardScoreResetS2CPacket(holderName, obj.getName())
                );
            }

            // 3. 准备并发送新条目
            List<Map.Entry<String, Integer>> sorted = item.getData().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(Global.config.getMaxDisplayNum())
                .toList();

            List<String> newHolders = new ArrayList<>();
            for (int i = 0; i < sorted.size(); i++) {
                Map.Entry<String, Integer> entry = sorted.get(i);
                int rank = i + 1;
                String holderName = "§" + String.format("%02x", rank) + entry.getKey();
                Text display = Text.literal("§e#" + rank + " §f" + entry.getKey() + " §7- §a" + entry.getValue());

                newHolders.add(holderName);
                player.networkHandler.sendPacket(
                    new ScoreboardScoreUpdateS2CPacket(
                        holderName,
                        obj.getName(),
                        entry.getValue(),
                        Optional.of(display),
                        Optional.empty()
                    )
                );
            }
            playerScoreHolders.put(uuid, newHolders);

            // 4. 设置 sidebar 显示
            player.networkHandler.sendPacket(
                new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, obj)
            );
        } catch (Exception e) {
            logger.error("failed to send custom display to player {}: {}", player.getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * 清除玩家的 sidebar 显示
     */
    public static void clearDisplay(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        ScoreboardObjective obj = scoreboard.getNullableObjective(getObjectiveId(player));
        if (obj != null) {
            // 清除所有条目
            List<String> holders = playerScoreHolders.getOrDefault(uuid, List.of());
            for (String holderName : holders) {
                player.networkHandler.sendPacket(
                    new ScoreboardScoreResetS2CPacket(holderName, obj.getName())
                );
            }
            // 清除 sidebar
            player.networkHandler.sendPacket(
                new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, (ScoreboardObjective) null)
            );
        }
        playerScoreHolders.remove(uuid);
    }

    /**
     * 玩家下线时清理
     */
    public static void onPlayerDisconnect(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        String objName = getObjectiveId(player);
        ScoreboardObjective obj = scoreboard.getNullableObjective(objName);
        if (obj != null) {
            scoreboard.removeObjective(obj);
        }
        playerScoreHolders.remove(uuid);
        cn.hycer.advancedscoreboard.Task.Task.removePlayer(uuid);
    }
}
