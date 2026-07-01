package cn.hycer.advancedscoreboard.mixin;

import cn.hycer.advancedscoreboard.Event.PlayerPlaceBlockEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 仅在方块被实际放置时计数，避免长按右键时重复计数。
 */
@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(method = "useOn", at = @At("RETURN"))
    private void onBlockPlaced(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() == InteractionResult.SUCCESS) {
            Player player = context.getPlayer();
            if (player != null) {
                PlayerPlaceBlockEvent.onPlace(player);
            }
        }
    }
}
