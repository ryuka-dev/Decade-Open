package com.tacz.guns.restriction;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Decade: one change of {@link GunUseRestriction}, server to client. */
public final class ServerMessageGunUseRestriction {
    private static final byte SLOTS = 0;
    private static final byte SUSPENDED = 1;
    private static final int ATTACK = 1;
    private static final int RELOAD = 1 << 1;

    private final byte kind;
    private final int value;

    private ServerMessageGunUseRestriction(byte kind, int value) {
        this.kind = kind;
        this.value = value;
    }

    static ServerMessageGunUseRestriction slots(int usableSlots) {
        return new ServerMessageGunUseRestriction(SLOTS, usableSlots);
    }

    static ServerMessageGunUseRestriction suspended(boolean attack, boolean reload) {
        return new ServerMessageGunUseRestriction(SUSPENDED, (attack ? ATTACK : 0) | (reload ? RELOAD : 0));
    }

    public static void encode(ServerMessageGunUseRestriction message, FriendlyByteBuf buf) {
        buf.writeByte(message.kind);
        buf.writeVarInt(message.value);
    }

    public static ServerMessageGunUseRestriction decode(FriendlyByteBuf buf) {
        byte kind = buf.readByte();
        int value = buf.readVarInt();
        boolean valid = switch (kind) {
            case SLOTS -> value >= 1 && value <= GunUseRestriction.ALL_SLOTS;
            case SUSPENDED -> (value & ~(ATTACK | RELOAD)) == 0;
            default -> false;
        };
        if (!valid) {
            throw new DecoderException("gun use restriction: kind " + kind + ", value " + value);
        }
        return new ServerMessageGunUseRestriction(kind, value);
    }

    public static void handle(ServerMessageGunUseRestriction message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> apply(message));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void apply(ServerMessageGunUseRestriction message) {
        if (message.kind == SLOTS) {
            ClientGunUseRestriction.acceptSlots(message.value);
        } else {
            ClientGunUseRestriction.acceptSuspended((message.value & ATTACK) != 0, (message.value & RELOAD) != 0);
        }
    }
}
