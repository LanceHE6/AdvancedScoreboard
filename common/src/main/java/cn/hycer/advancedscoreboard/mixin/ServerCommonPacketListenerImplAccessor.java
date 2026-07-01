package cn.hycer.advancedscoreboard.mixin;

import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露 ServerCommonPacketListenerImpl 中 private 的 latency 字段，
 * 用于获取玩家延迟（ping，单位 ms）。
 */
@Mixin(ServerCommonPacketListenerImpl.class)
public interface ServerCommonPacketListenerImplAccessor {

    @Accessor("latency")
    int getLatency();
}
