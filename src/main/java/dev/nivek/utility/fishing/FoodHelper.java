package dev.nivek.utility.fishing;

import dev.nivek.utility.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

/**
 * Atalho de comer: só age quando o jogador aperta a tecla. Escolhe o alimento na hotbar
 * (favorito, depois o item da mão, depois o primeiro), come e volta para o slot anterior.
 */
public final class FoodHelper {
    private static boolean active = false;
    private static int prevSlot = -1;
    private static int ticks = 0;
    private static int startCount = 0;
    private static int startFood = 0;
    private static int slot = -1;

    private FoodHelper() {
    }

    public static void start(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || active || p.isUsingItem()) return;
        int s = findFoodSlot(p);
        if (s < 0) {
            p.sendMessage(Text.literal("Sem comida utilizável na hotbar."), true);
            return;
        }
        PlayerInventory inv = p.getInventory();
        prevSlot = inv.selectedSlot;
        slot = s;
        inv.selectedSlot = s;
        startCount = inv.getStack(s).getCount();
        startFood = p.getHungerManager().getFoodLevel();
        mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
        mc.options.useKey.setPressed(true);
        active = true;
        ticks = 0;
    }

    public static void tick(MinecraftClient mc) {
        if (!active) return;
        ClientPlayerEntity p = mc.player;
        if (p == null) {
            active = false;
            return;
        }
        ticks++;
        ItemStack now = p.getInventory().getStack(slot);
        boolean finished = now.getCount() < startCount || p.getHungerManager().getFoodLevel() > startFood;
        boolean notStarted = ticks > 6 && !p.isUsingItem();
        if (finished || notStarted || ticks > 100 || mc.currentScreen != null) {
            mc.options.useKey.setPressed(false);
            if (prevSlot >= 0) p.getInventory().selectedSlot = prevSlot;
            active = false;
        }
    }

    private static boolean edible(ClientPlayerEntity p, ItemStack s) {
        FoodComponent f = s.get(DataComponentTypes.FOOD);
        return f != null && p.canConsume(f.canAlwaysEat());
    }

    private static int findFoodSlot(ClientPlayerEntity p) {
        PlayerInventory inv = p.getInventory();
        String fav = ConfigManager.get().favoriteFood;
        if (fav != null && !fav.isEmpty()) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = inv.getStack(i);
                if (edible(p, s) && Registries.ITEM.getId(s.getItem()).toString().equals(fav)) return i;
            }
        }
        if (edible(p, inv.getStack(inv.selectedSlot))) return inv.selectedSlot;
        for (int i = 0; i < 9; i++) {
            if (edible(p, inv.getStack(i))) return i;
        }
        return -1;
    }
}
