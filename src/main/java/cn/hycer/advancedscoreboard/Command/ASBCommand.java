package cn.hycer.advancedscoreboard.Command;

import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.text.Text;

import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ASBCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("asb")
                    .then(literal("set")
                        .requires(source -> {
                            PermissionPredicate perms = source.getPermissions();
                            if (perms instanceof LeveledPermissionPredicate leveled) {
                                return leveled.getLevel().isAtLeast(PermissionLevel.OWNERS);
                            }
                            return true;
                        })
                        .then(literal("switchInterval")
                            .then(argument("value", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    int value = IntegerArgumentType.getInteger(context, "value");
                                    Global.config.setSwitchInterval(value);
                                    Global.config.saveConfig();
                                    context.getSource().sendFeedback(
                                        () -> Text.literal("轮播间隔已设置为 " + value + " 秒"),
                                        false
                                    );
                                    return 1;
                                })
                            )
                        )
                        .then(literal("saveInterval")
                            .then(argument("value", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    int value = IntegerArgumentType.getInteger(context, "value");
                                    Global.config.setSaveInterval(value);
                                    Global.config.saveConfig();
                                    context.getSource().sendFeedback(
                                        () -> Text.literal("保存间隔已设置为 " + value + " 秒"),
                                        false
                                    );
                                    return 1;
                                })
                            )
                        )
                        .then(literal("maxDisplayNum")
                            .then(argument("value", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    int value = IntegerArgumentType.getInteger(context, "value");
                                    Global.config.setMaxDisplayNum(value);
                                    Global.config.saveConfig();
                                    context.getSource().sendFeedback(
                                        () -> Text.literal("最大显示数量已设置为 " + value),
                                        false
                                    );
                                    return 1;
                                })
                            )
                        )
                    )
                    .then(literal("scoreboard")
                        .then(argument("displayName", StringArgumentType.greedyString())
                            .suggests((context, builder) -> {
                                for (ScoreboardItem item : Global.config.getScoreboards()) {
                                    builder.suggest(item.getDisplayName());
                                }
                                return builder.buildFuture();
                            })
                            .executes(context -> {
                                String displayName = StringArgumentType.getString(context, "displayName");
                                ScoreboardItem item = Global.config.getScoreboards().stream()
                                    .filter(sb -> displayName.equals(sb.getDisplayName()))
                                    .findFirst()
                                    .orElse(null);

                                if (item == null) {
                                    context.getSource().sendFeedback(
                                        () -> Text.literal("未找到榜单: " + displayName),
                                        false
                                    );
                                    return 0;
                                }

                                context.getSource().sendFeedback(
                                    () -> Text.literal("=== " + item.getDisplayName() + " ==="),
                                    false
                                );

                                if (item.getData().isEmpty()) {
                                    context.getSource().sendFeedback(
                                        () -> Text.literal("暂无数据"),
                                        false
                                    );
                                } else {
                                    item.getData().entrySet().stream()
                                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                        .forEach(entry ->
                                            context.getSource().sendFeedback(
                                                () -> Text.literal(entry.getKey() + " - " + entry.getValue()),
                                                false
                                            )
                                        );
                                }

                                return 1;
                            })
                        )
                    )
                    .then(literal("notDisplay")
                        .then(argument("displayName", StringArgumentType.greedyString())
                            .suggests((context, builder) -> {
                                for (ScoreboardItem item : Global.config.getScoreboards()) {
                                    builder.suggest(item.getDisplayName());
                                }
                                return builder.buildFuture();
                            })
                            .executes(context -> {
                                String displayName = StringArgumentType.getString(context, "displayName");
                                ScoreboardItem item = Global.config.getScoreboards().stream()
                                    .filter(sb -> displayName.equals(sb.getDisplayName()))
                                    .findFirst()
                                    .orElse(null);

                                if (item == null) {
                                    context.getSource().sendFeedback(
                                        () -> Text.literal("未找到榜单: " + displayName),
                                        false
                                    );
                                    return 0;
                                }

                                String playerName = context.getSource().getName();
                                boolean hidden = Global.config.toggleScoreboardVisibility(playerName, item.getInternalName());
                                Global.config.saveConfig();

                                String message = hidden
                                    ? "已隐藏榜单: " + item.getDisplayName()
                                    : "已显示榜单: " + item.getDisplayName();
                                context.getSource().sendFeedback(
                                    () -> Text.literal(message),
                                    false
                                );

                                return 1;
                            })
                        )
                    )
            );
        });
    }
}
