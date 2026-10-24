package com.example.dripfall;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Ustawienia wysyłane z menu klienta do serwera (działa też w singleplayerze). */
public record SettingsPayload(boolean enabled, float speed, int radius, int interval,
                              boolean onlyOpen) implements CustomPayload {

    public static final Id<SettingsPayload> ID = new Id<>(Identifier.of(DripFall.MOD_ID, "settings"));

    public static final PacketCodec<ByteBuf, SettingsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, SettingsPayload::enabled,
            PacketCodecs.FLOAT, SettingsPayload::speed,
            PacketCodecs.INTEGER, SettingsPayload::radius,
            PacketCodecs.INTEGER, SettingsPayload::interval,
            PacketCodecs.BOOLEAN, SettingsPayload::onlyOpen,
            SettingsPayload::new);

    public static SettingsPayload defaults() {
        return new SettingsPayload(false, 3.0f, 8, 5, false);
    }

    /** Serwer nie ufa klientowi - przycina wartości do bezpiecznych zakresów. */
    public SettingsPayload clamped() {
        return new SettingsPayload(enabled,
                MathHelper.clamp(speed, 0.5f, 10.0f),
                MathHelper.clamp(radius, 2, 16),
                MathHelper.clamp(interval, 1, 40),
                onlyOpen);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
