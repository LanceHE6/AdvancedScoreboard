package cn.hycer.advancedscoreboard.Command;

import cn.hycer.advancedscoreboard.Event.ServerStartedEvent;
import cn.hycer.advancedscoreboard.Global.Global;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SetCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return literal("set")
            .requires(source -> {
                PermissionPredicate perms = source.getPermissions();
                if (perms instanceof LeveledPermissionPredicate leveled) {
                    return leveled.getLevel().isAtLeast(PermissionLevel.GAMEMASTERS);
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
            .then(literal("border")
                .then(argument("value", StringArgumentType.word())
                    .executes(context -> {
                        String value = StringArgumentType.getString(context, "value");
                        Global.config.setBorder(value);
                        Global.config.saveConfig();
                        ServerStartedEvent.refreshAllDisplayNames();
                        context.getSource().sendFeedback(
                            () -> Text.literal("边框已设置为 " + value),
                            false
                        );
                        return 1;
                    })
                )
            );
    }
}
