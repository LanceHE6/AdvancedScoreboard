package cn.hycer.advancedscoreboard.Command;

import cn.hycer.advancedscoreboard.Global.Global;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ASBCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("asb")
                    .requires(source -> {
                        PermissionPredicate perms = source.getPermissions();
                        if (perms instanceof LeveledPermissionPredicate leveled) {
                            return leveled.getLevel().isAtLeast(PermissionLevel.OWNERS);
                        }
                        return true;
                    })
                    .then(literal("set")
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
            );
        });
    }
}
