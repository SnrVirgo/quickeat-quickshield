package quickeat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import quickeat.mixin.KeyMappingAccessor;

public class QuickEatClient implements ClientModInitializer {
    private enum Mode { NONE, ONESHOT, SHIELD, HOLD, CROSSBOW }

    private static Mode mode = Mode.NONE;
    private static boolean pressedThisTick = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(QuickEatClient::onStartTick);
        ClientTickEvents.END_CLIENT_TICK.register(QuickEatClient::onEndTick);
    }

    private static void onStartTick(Minecraft mc) {
        pressedThisTick = false;
        LocalPlayer player = mc.player;
        if (player == null || !player.isAlive() || mc.gui.screen() != null) {
            stop(mc);
            return;
        }

        KeyMappingAccessor use = (KeyMappingAccessor) mc.options.keyUse;
        KeyMappingAccessor attack = (KeyMappingAccessor) mc.options.keyAttack;
        boolean pressed = use.quickeat$getClickCount() > 0;

        if (mode == Mode.NONE) {
            pressedThisTick = pressed;
            return;
        }

        if (pressed) {
            use.quickeat$setClickCount(0);
            stop(mc);
            return;
        }
        if (mode == Mode.SHIELD && attack.quickeat$getClickCount() > 0) {
            stop(mc);
            return;
        }
        mc.options.keyUse.setDown(true);
    }

    private static void onEndTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || !player.isAlive() || mc.gui.screen() != null) {
            stop(mc);
            return;
        }

        if (mode == Mode.NONE) {
            if (pressedThisTick && player.isUsingItem()) {
                mode = classify(player.getUseItem());
                if (mode != Mode.NONE) {
                    mc.options.keyUse.setDown(true);
                }
            }
            pressedThisTick = false;
            return;
        }

        switch (mode) {
            case ONESHOT, HOLD -> {
                if (!player.isUsingItem()) {
                    stop(mc);
                    return;
                }
            }
            case SHIELD -> {
                if (!player.isUsingItem() && !hasShield(player)) {
                    stop(mc);
                    return;
                }
            }
            case CROSSBOW -> {
                if (!player.isUsingItem()) {
                    stop(mc);
                    return;
                }
                ItemStack crossbow = player.getUseItem();
                if (player.getTicksUsingItem() >= CrossbowItem.getChargeDuration(crossbow, player)) {
                    stop(mc);
                    return;
                }
            }
            default -> {
            }
        }
        mc.options.keyUse.setDown(true);
    }

    private static Mode classify(ItemStack stack) {
        if (stack.isEmpty()) return Mode.NONE;
        if (stack.get(DataComponents.FOOD) != null) return Mode.ONESHOT;
        return switch (stack.getUseAnimation().name()) {
            case "EAT", "DRINK", "TOOT_HORN", "BRUSH" -> Mode.ONESHOT;
            case "BLOCK" -> Mode.SHIELD;
            case "BOW", "SPEAR", "TRIDENT", "SPYGLASS" -> Mode.HOLD;
            case "CROSSBOW" -> Mode.CROSSBOW;
            default -> Mode.NONE;
        };
    }

    private static void stop(Minecraft mc) {
        pressedThisTick = false;
        if (mode != Mode.NONE) {
            mode = Mode.NONE;
            if (mc.options != null) {
                mc.options.keyUse.setDown(false);
            }
        }
    }

    private static boolean isShield(ItemStack stack) {
        return !stack.isEmpty() && stack.getUseAnimation() == ItemUseAnimation.BLOCK;
    }

    private static boolean hasShield(LocalPlayer player) {
        return isShield(player.getMainHandItem()) || isShield(player.getOffhandItem());
    }
}
