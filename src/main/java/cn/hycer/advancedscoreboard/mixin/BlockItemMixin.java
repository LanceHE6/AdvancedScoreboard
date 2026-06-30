package cn.hycer.advancedscoreboard.mixin;

import cn.hycer.advancedscoreboard.Event.PlayerPlaceBlockEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 仅在方块被实际放置时计数，避免长按右键时重复计数。
 */
@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(method = "useOnBlock", at = @At("RETURN"))
    private void onBlockPlaced(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (cir.getReturnValue() == ActionResult.SUCCESS) {
            PlayerEntity player = context.getPlayer();
            if (player != null) {
                PlayerPlaceBlockEvent.onPlace(player);
            }
        }
    }
}
