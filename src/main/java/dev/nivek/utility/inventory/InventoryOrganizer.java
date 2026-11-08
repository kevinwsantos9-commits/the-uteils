package dev.nivek.utility.inventory;

import dev.nivek.utility.config.ServerConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Organização feita no servidor, sem nunca descartar itens: só junta pilhas iguais e reordena.
 * Hotbar, armadura e offhand não são tocados.
 */
public final class InventoryOrganizer {
    private InventoryOrganizer() {
    }

    public static void handle(ServerPlayerEntity p, int mode) {
        if (!ServerConfig.get().allowOrganize || p.isSpectator()) return;
        switch (mode) {
            case 0 -> sortPlayer(p);
            case 1 -> sortContainer(p);
            case 2 -> moveMatching(p);
            default -> {
            }
        }
    }

    private static void sortPlayer(ServerPlayerEntity p) {
        PlayerInventory inv = p.getInventory();
        List<ItemStack> items = new ArrayList<>();
        for (int i = 9; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty()) items.add(s.copy());
        }
        List<ItemStack> sorted = mergeAndSort(items, 64);
        for (int i = 9; i < 36; i++) {
            int idx = i - 9;
            inv.setStack(i, idx < sorted.size() ? sorted.get(idx) : ItemStack.EMPTY);
        }
        inv.markDirty();
        p.currentScreenHandler.sendContentUpdates();
        p.playerScreenHandler.sendContentUpdates();
    }

    private static Inventory openContainer(ServerPlayerEntity p) {
        ScreenHandler h = p.currentScreenHandler;
        if (!(h instanceof GenericContainerScreenHandler || h instanceof ShulkerBoxScreenHandler)) return null;
        if (h.slots.isEmpty()) return null;
        Inventory c = h.slots.get(0).inventory;
        if (c == p.getInventory() || !c.canPlayerUse(p)) return null;
        return c;
    }

    private static void sortContainer(ServerPlayerEntity p) {
        Inventory c = openContainer(p);
        if (c == null) {
            p.sendMessage(Text.literal("Abra um baú, barril ou shulker para organizar."), true);
            return;
        }
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < c.size(); i++) {
            ItemStack s = c.getStack(i);
            if (!s.isEmpty()) items.add(s.copy());
        }
        List<ItemStack> sorted = mergeAndSort(items, c.getMaxCountPerStack());
        for (int i = 0; i < c.size(); i++) {
            c.setStack(i, i < sorted.size() ? sorted.get(i) : ItemStack.EMPTY);
        }
        c.markDirty();
        p.currentScreenHandler.sendContentUpdates();
    }

    /** Move do inventário (fora da hotbar) para o baú os itens que o baú já possui. */
    private static void moveMatching(ServerPlayerEntity p) {
        Inventory c = openContainer(p);
        if (c == null) {
            p.sendMessage(Text.literal("Abra um baú, barril ou shulker primeiro."), true);
            return;
        }
        PlayerInventory inv = p.getInventory();
        for (int i = 9; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty() || !containerHas(c, s)) continue;
            inv.setStack(i, insert(c, s.copy()));
        }
        c.markDirty();
        inv.markDirty();
        p.currentScreenHandler.sendContentUpdates();
    }

    private static boolean containerHas(Inventory c, ItemStack s) {
        for (int i = 0; i < c.size(); i++) {
            ItemStack t = c.getStack(i);
            if (!t.isEmpty() && ItemStack.areItemsAndComponentsEqual(t, s)) return true;
        }
        return false;
    }

    /** Insere o máximo possível e devolve o que sobrou. */
    private static ItemStack insert(Inventory c, ItemStack stack) {
        ItemStack rem = stack.copy();
        int cap = c.getMaxCountPerStack();
        for (int i = 0; i < c.size() && !rem.isEmpty(); i++) {
            ItemStack t = c.getStack(i);
            if (!t.isEmpty() && ItemStack.areItemsAndComponentsEqual(t, rem)) {
                int max = Math.min(t.getMaxCount(), cap);
                int mv = Math.min(rem.getCount(), max - t.getCount());
                if (mv > 0) {
                    t.increment(mv);
                    rem.decrement(mv);
                }
            }
        }
        for (int i = 0; i < c.size() && !rem.isEmpty(); i++) {
            if (c.getStack(i).isEmpty() && c.isValid(i, rem)) {
                int mv = Math.min(rem.getCount(), Math.min(rem.getMaxCount(), cap));
                c.setStack(i, rem.copyWithCount(mv));
                rem.decrement(mv);
            }
        }
        return rem.isEmpty() ? ItemStack.EMPTY : rem;
    }

    static List<ItemStack> mergeAndSort(List<ItemStack> in, int cap) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : in) {
            ItemStack cur = s.copy();
            for (ItemStack e : out) {
                if (cur.isEmpty()) break;
                if (ItemStack.areItemsAndComponentsEqual(e, cur)) {
                    int max = Math.min(e.getMaxCount(), cap);
                    int mv = Math.min(cur.getCount(), max - e.getCount());
                    if (mv > 0) {
                        e.increment(mv);
                        cur.decrement(mv);
                    }
                }
            }
            if (!cur.isEmpty()) out.add(cur);
        }
        out.sort(Comparator.<ItemStack>comparingInt(InventoryOrganizer::category)
                .thenComparing(s -> Registries.ITEM.getId(s.getItem()).toString())
                .thenComparing((a, b) -> Integer.compare(b.getCount(), a.getCount())));
        return out;
    }

    /** 0 ferramentas/armas, 1 armaduras, 2 comida, 3 blocos, 4 recursos e o resto. */
    private static int category(ItemStack s) {
        if (s.getItem() instanceof ArmorItem) return 1;
        if (s.isDamageable()) return 0;
        if (s.get(DataComponentTypes.FOOD) != null) return 2;
        if (s.getItem() instanceof BlockItem) return 3;
        return 4;
    }
}
