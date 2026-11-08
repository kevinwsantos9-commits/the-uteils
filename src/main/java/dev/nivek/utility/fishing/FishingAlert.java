package dev.nivek.utility.fishing;

import dev.nivek.utility.config.ConfigManager;
import dev.nivek.utility.mixin.FishingBobberEntityAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Avisa quando um peixe morde (lido do estado real do anzol) e quando a comida está baixa. */
public final class FishingAlert {
    private static boolean wasBiting = false;
    private static boolean lowFoodShown = false;

    private FishingAlert() {
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null) {
            wasBiting = false;
            lowFoodShown = false;
            return;
        }
        var cfg = ConfigManager.get();

        FishingBobberEntity hook = mc.player.fishHook;
        boolean biting = hook != null && ((FishingBobberEntityAccessor) hook).nivek_isCaughtFish();
        if (biting && !wasBiting && cfg.fishAlert) {
            mc.player.sendMessage(Text.literal("Peixe fisgado!").formatted(Formatting.RED, Formatting.BOLD), true);
            if (cfg.fishSound) mc.player.playSound(SoundEvents.BLOCK_BELL_USE, 1.0F, 1.2F);
        }
        wasBiting = biting;

        int food = mc.player.getHungerManager().getFoodLevel();
        if (cfg.lowFoodAlert && food <= cfg.lowFoodThreshold) {
            if (!lowFoodShown) {
                lowFoodShown = true;
                mc.player.sendMessage(Text.literal("Pouca comida! (" + food + "/20)").formatted(Formatting.GOLD, Formatting.BOLD), true);
                mc.player.playSound(SoundEvents.ENTITY_PLAYER_BURP, 0.6F, 0.7F);
            }
        } else if (food > cfg.lowFoodThreshold) {
            lowFoodShown = false;
        }
    }
}
