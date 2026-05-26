package cn.hycer.advancedscoreboard.Command;

import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ScoreboardCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return literal("scoreboard")
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

                    context.getSource().sendFeedback(
                        () -> Text.literal(Global.config.getFormattedDisplayName(item)),
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
            );
    }
}
