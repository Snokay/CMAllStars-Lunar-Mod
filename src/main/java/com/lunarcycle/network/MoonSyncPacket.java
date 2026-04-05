package com.lunarcycle.network;

import com.lunarcycle.moon.MoonType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * S→C packet — tells the client which moon is active for sky color rendering.
 * Uses Fabric 1.21.1 typed payload API.
 */
public record MoonSyncPacket(MoonType moon) implements CustomPayload {

    public static final CustomPayload.Id<MoonSyncPacket> ID =
            new CustomPayload.Id<>(Identifier.of("lunarcycle", "moon_sync"));

    public static final PacketCodec<PacketByteBuf, MoonSyncPacket> CODEC =
            PacketCodec.of(
                (packet, buf) -> buf.writeEnumConstant(packet.moon()),
                buf -> new MoonSyncPacket(buf.readEnumConstant(MoonType.class))
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    // ── Server side ──────────────────────────────────────────────────────────

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }

    public static void send(ServerPlayerEntity player, MoonType moon) {
        ServerPlayNetworking.send(player, new MoonSyncPacket(moon));
    }

}
