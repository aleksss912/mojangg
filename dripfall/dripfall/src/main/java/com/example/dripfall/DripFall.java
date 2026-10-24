package com.example.dripfall;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.Thickness;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DripFall implements ModInitializer {
    public static final String MOD_ID = "dripfall";

    /** Maks. liczba stalaktytów tworzonych na jeden "puls" na gracza (ochrona przed lagiem). */
    private static final int MAX_PER_PULSE = 24;

    private static final Map<UUID, SettingsPayload> SETTINGS = new HashMap<>();
    private static final List<Tracked> TRACKED = new ArrayList<>();

    private record Tracked(FallingBlockEntity entity, float speed) {}

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(SettingsPayload.ID, SettingsPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SettingsPayload.ID,
                (payload, context) -> SETTINGS.put(context.player().getUuid(), payload.clamped()));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                SETTINGS.remove(handler.player.getUuid()));

        ServerTickEvents.END_SERVER_TICK.register(DripFall::tick);
    }

    private static void tick(MinecraftServer server) {
        // 1) Utrzymuj dużą prędkość spadania (vanilla ma limit ok. 3.9 bloku/tick przez opór).
        Iterator<Tracked> it = TRACKED.iterator();
        while (it.hasNext()) {
            Tracked t = it.next();
            FallingBlockEntity e = t.entity();
            if (e.isRemoved()) {
                it.remove();
            } else if (!e.isOnGround()) {
                e.setVelocity(0, -t.speed(), 0);
            }
        }

        // 2) Zrzucaj nowe stalaktyty nad trapdorami wokół graczy.
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            SettingsPayload s = SETTINGS.get(player.getUuid());
            if (s == null || !s.enabled()) continue;
            if (server.getTicks() % s.interval() != 0) continue;
            spawnAround(player, s);
        }
    }

    private static void spawnAround(ServerPlayerEntity player, SettingsPayload s) {
        ServerWorld world = player.getServerWorld();
        BlockPos c = player.getBlockPos();
        int r = s.radius();
        int spawned = 0;

        BlockState dripstone = Blocks.POINTED_DRIPSTONE.getDefaultState()
                .with(PointedDripstoneBlock.VERTICAL_DIRECTION, Direction.DOWN)
                .with(PointedDripstoneBlock.THICKNESS, Thickness.TIP);

        for (BlockPos pos : BlockPos.iterate(c.add(-r, -r, -r), c.add(r, r, r))) {
            if (spawned >= MAX_PER_PULSE) break;

            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof TrapdoorBlock)) continue;
            if (s.onlyOpen() && !state.get(TrapdoorBlock.OPEN)) continue;

            // Stalaktyt wylatuje z klapy: pojawia sie tuz pod nia i od razu leci w dol.
            BlockPos below = pos.down();
            if (!world.getBlockState(below).isAir()) continue;

            FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, below, dripstone);
            fb.setHurtEntities(2.0f, 40);
            fb.setDestroyedOnLanding();
            fb.setVelocity(0, -s.speed(), 0);
            TRACKED.add(new Tracked(fb, s.speed()));
            spawned++;
        }
    }
}
