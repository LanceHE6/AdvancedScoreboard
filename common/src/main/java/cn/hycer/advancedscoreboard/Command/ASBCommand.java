package cn.hycer.advancedscoreboard.Command;

import cn.hycer.advancedscoreboard.Config.ScoreboardItem;
import cn.hycer.advancedscoreboard.Global.Global;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.literal;

public class ASBCommand {

    static final SuggestionProvider<CommandSourceStack> DISPLAY_NAME_SUGGESTIONS =
        (context, builder) -> {
            for (ScoreboardItem item : Global.config.getScoreboards()) {
                builder.suggest(item.getDisplayName());
            }
            return builder.buildFuture();
        };

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("asb")
                    .then(SetCommand.build())
                    .then(ScoreboardCommand.build())
                    .then(NotDisplayCommand.build())
            );
        });
    }
}
