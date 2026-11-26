package com.example.safecreative;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = SafeCreative.MOD_ID, dist = Dist.CLIENT)
public final class SafeCreative {
    public static final String MOD_ID = "safecreative";

    private static final String KEY_CATEGORY = "key.categories.safecreative";
    private static final KeyMapping DROPS = toggleKey("key.safecreative.original_drops", GLFW.GLFW_KEY_J);
    private static final KeyMapping PROTECT = toggleKey("key.safecreative.container_protection", GLFW.GLFW_KEY_K);

    public SafeCreative(IEventBus modBus) {
        modBus.addListener(SafeCreative::registerKeys);
        NeoForge.EVENT_BUS.addListener(SafeCreative::onKey);
        NeoForge.EVENT_BUS.addListener(SafeCreative::onMouse);
        NeoForge.EVENT_BUS.addListener(SafeCreative::afterTick);
        NeoForge.EVENT_BUS.addListener(SafeCreative::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(SafeCreative::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(ContainerProtection::onAttack);
    }

    private static KeyMapping toggleKey(String name, int key) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, KeyModifier.ALT,
                InputConstants.Type.KEYSYM, key, KEY_CATEGORY);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(DROPS);
        event.register(PROTECT);
    }

    private static void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            handleToggle(InputConstants.getKey(event.getKey(), event.getScanCode()));
        }
    }

    private static void onMouse(InputEvent.MouseButton.Post event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            handleToggle(InputConstants.Type.MOUSE.getOrCreate(event.getButton()));
        }
    }

    private static void handleToggle(InputConstants.Key key) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.getOverlay() != null
                || minecraft.player == null || !minecraft.player.isCreative()) {
            return;
        }
        if (DROPS.isActiveAndMatches(key)) {
            showToggle(minecraft, "message.safecreative.original_drops", OriginalBlockDrops.toggle());
        }
        if (PROTECT.isActiveAndMatches(key)) {
            showToggle(minecraft, "message.safecreative.container_protection", ContainerProtection.toggle());
        }
    }

    private static void showToggle(Minecraft minecraft, String feature, boolean enabled) {
        minecraft.gui.setOverlayMessage(Component.translatable(feature,
                Component.translatable(enabled ? "message.safecreative.on" : "message.safecreative.off")), false);
    }

    private static void afterTick(ClientTickEvent.Post event) {
        // Raw PRESS handles toggling; discard queued clicks, including keyboard repeat.
        while (DROPS.consumeClick()) { }
        while (PROTECT.consumeClick()) { }
        SafePick.afterTick();
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        SafePick.cancel();
    }

    private static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        // A scroll can change selection before the next tick, even if it is later scrolled back.
        SafePick.cancel();
    }
}
