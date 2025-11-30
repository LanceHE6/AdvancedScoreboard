// ScoreboardCommand.java
package cn.hycer.advancedscoreboard;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.render.command.ModelPartCommandRenderer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ScoreboardCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(ModelPartCommandRenderer.Commands.literal("scoreboard")
                .then(Commands.literal("toggle")
                        .then(Commands.argument("stat", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (StatType type : StatType.values()) {
                                        builder.suggest(type.getId());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> toggleStat(context, StringArgumentType.getString(context, "stat")))
                        )
                )
                .then(Commands.literal("list")
                        .executes(ScoreboardCommand::listStats)
                )
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(2)) // OP权限
                        .then(Commands.literal("enable")
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> setStatEnabled(context,
                                                        StringArgumentType.getString(context, "stat"),
                                                        BoolArgumentType.getBool(context, "enabled")))
                                        )
                                )
                        )
                        .then(Commands.literal("settitle")
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                                .executes(context -> setCustomTitle(context,
                                                        StringArgumentType.getString(context, "stat"),
                                                        StringArgumentType.getString(context, "title")))
                                        )
                                )
                        )
                        .then(Commands.literal("setdata")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("stat", StringArgumentType.word())
                                                .then(Commands.argument("value", IntegerArgumentType.integer())
                                                        .executes(context -> setPlayerData(context,
                                                                EntityArgument.getPlayer(context, "player"),
                                                                StringArgumentType.getString(context, "stat"),
                                                                IntegerArgumentType.getInteger(context, "value")))
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int toggleStat(CommandContext<CommandSourceStack> context, String statId) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        StatType type = StatType.fromId(statId);
        if (type == null) {
            context.getSource().sendFailure(Component.literal("未知的统计项: " + statId));
            return 0;
        }

        Set<StatType> currentStats = AdvancedScoreboard.scoreboardManager.getPlayerVisibleStats(player.getUUID());
        Set<StatType> newStats = new HashSet<>(currentStats);

        if (newStats.contains(type)) {
            newStats.remove(type);
            context.getSource().sendSuccess(() -> Component.literal("已隐藏统计项: " + type.getDisplayName()), false);
        } else {
            newStats.add(type);
            context.getSource().sendSuccess(() -> Component.literal("已显示统计项: " + type.getDisplayName()), false);
        }

        AdvancedScoreboard.scoreboardManager.setPlayerVisibleStats(player.getUUID(), newStats);
        return Command.SINGLE_SUCCESS;
    }

    private static int listStats(CommandContext<CommandSourceStack> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        Set<StatType> visibleStats = AdvancedScoreboard.scoreboardManager.getPlayerVisibleStats(player.getUUID());
        String statsList = visibleStats.stream()
                .map(StatType::getDisplayName)
                .collect(Collectors.joining(", "));

        context.getSource().sendSuccess(() -> Component.literal("当前显示的统计项: " + statsList), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setStatEnabled(CommandContext<CommandSourceStack> context, String statId, boolean enabled) {
        StatType type = StatType.fromId(statId);
        if (type == null) {
            context.getSource().sendFailure(Component.literal("未知的统计项: " + statId));
            return 0;
        }

        AdvancedScoreboard.configManager.setStatEnabled(type, enabled);
        context.getSource().sendSuccess(() -> Component.literal((enabled ? "启用" : "禁用") + "统计项: " + type.getDisplayName()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setCustomTitle(CommandContext<CommandSourceStack> context, String statId, String title) {
        StatType type = StatType.fromId(statId);
        if (type == null) {
            context.getSource().sendFailure(Component.literal("未知的统计项: " + statId));
            return 0;
        }

        AdvancedScoreboard.configManager.setCustomTitle(type, title);
        context.getSource().sendSuccess(() -> Component.literal("设置统计项标题: " + type.getDisplayName() + " -> " + title), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setPlayerData(CommandContext<CommandSourceStack> context, ServerPlayerEntity targetPlayer, String statId, int value) {
        // 这里需要根据统计项类型实现具体的数据设置逻辑
        context.getSource().sendSuccess(() -> Component.literal("已设置玩家 " + targetPlayer.getScoreboardName() + " 的 " + statId + " 为 " + value), true);
        return Command.SINGLE_SUCCESS;
    }
}