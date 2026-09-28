package me.xjqsh.lrtactical.network.message;

import me.xjqsh.lrtactical.api.item.IConsumable;
import me.xjqsh.lrtactical.api.item.IThrowable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CCancelItemUse {
    public CCancelItemUse() {
    }

    public CCancelItemUse(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            // any consumable or throwable: the client lets go of a hold use this way when the
            // player cancels it, and stopping here, unlike a release, throws nothing. Switching
            // slots already stops a use the same way, so this lets the client do nothing new.
            ItemStack useItem = player.getUseItem();
            if (useItem.getItem() instanceof IConsumable || useItem.getItem() instanceof IThrowable) {
                player.stopUsingItem();
            }
        });
        context.setPacketHandled(true);
    }
}
