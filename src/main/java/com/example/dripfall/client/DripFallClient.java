package com.example.dripfall.client;

import com.example.dripfall.SettingsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DripFallClient implements ClientModInitializer {
    /** Ustawienia dripstone (wysylane do serwera z zainstalowanym modem / dla singleplayera). */
    public static SettingsPayload current = SettingsPayload.defaults();

    /** Ustawienia szybkich klap (czysto kliencki tryb, dziala na kazdym serwerze). */
    public static boolean flickEnabled = false;
    public static int flickInterval = 4;     // co ile tickow
    public static float flickRange = 4.0f;   // w blokach (zasieg gry ~4.5)

    private static final int MAX_PER_PULSE = 4;
    private static int cooldown = 0;

    private static KeyBinding openMenu;
    private static KeyBinding toggleFlick;

    @Override
    public void onInitializeClient() {
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dripfall.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.dripfall"));
        toggleFlick = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dripfall.flick", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.dripfall"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.wasPressed()) {
                client.setScreen(new ConfigScreen(client.currentScreen));
            }
            while (toggleFlick.wasPressed()) {
                flickEnabled = !flickEnabled;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("DripFall: szybkie klapy "
                            + (flickEnabled ? "WŁĄCZONE" : "wyłączone")), true);
                }
            }
            flickTick(client);
        });

        // Po wejsciu na swiat wyslij zapamietane ustawienia (nic nie robi, jesli serwer nie ma moda).
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> send());
    }

    public static void send() {
        if (ClientPlayNetworking.canSend(SettingsPayload.ID)) {
            ClientPlayNetworking.send(current);
        }
    }

    /** Co kilka tickow "klika" najblizsze klapy w zasiegu, wiec otwieraja sie i zamykaja bez przerwy. */
    private static void flickTick(MinecraftClient c) {
        if (!flickEnabled || c.player == null || c.world == null || c.interactionManager == null) return;
        if (c.currentScreen != null) return;
        if (++cooldown < flickInterval) return;
        cooldown = 0;

        ClientPlayerEntity p = c.player;
        if (p.isSneaking()) return; // przy kucaniu klik moglby postawic blok zamiast przelaczyc klape

        ClientWorld w = c.world;
        Vec3d eye = p.getEyePos();
        double r = flickRange;
        int ri = (int) Math.ceil(r);
        BlockPos base = p.getBlockPos();

        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.iterate(base.add(-ri, -ri, -ri), base.add(ri, ri, ri))) {
            BlockState st = w.getBlockState(pos);
            if (!(st.getBlock() instanceof TrapdoorBlock) || st.isOf(Blocks.IRON_TRAPDOOR)) continue;
            if (Vec3d.ofCenter(pos).squaredDistanceTo(eye) > r * r) continue;
            found.add(pos.toImmutable());
        }
        found.sort(Comparator.comparingDouble(pos -> Vec3d.ofCenter(pos).squaredDistanceTo(eye)));

        int n = 0;
        for (BlockPos pos : found) {
            if (n++ >= MAX_PER_PULSE) break;
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
            c.interactionManager.interactBlock(p, Hand.MAIN_HAND, hit);
        }
    }
}
