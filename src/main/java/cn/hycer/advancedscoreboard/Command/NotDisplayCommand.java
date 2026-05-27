package cn.hycer.advancedscoreboard.Command;

import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class NotDisplayCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return literal("notDisplay")
            .requires(source -> source.hasPermissionLevel(2))
            .then(argument("displayName", StringArgumentType.greedyString())
                .suggests(ASBCommand.DISPLAY_NAME_SUGGESTIONS)
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

                    boolean hidden = Global.config.toggleScoreboardVisibility(item.getInternalName());
                    Global.config.saveConfig();

                    String message = hidden
                        ? "已全局隐藏榜单: " + item.getDisplayName()
                        : "已全局显示榜单: " + item.getDisplayName();
                    context.getSource().sendFeedback(
                        () -> Text.literal(message),
                        false
                    );

                    return 1;
                })
            );
    }
}
