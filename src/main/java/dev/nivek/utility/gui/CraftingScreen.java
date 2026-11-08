package dev.nivek.utility.gui;

import dev.nivek.utility.util.Colors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Assistente de crafting: só consulta. Mostra materiais, quanto você tem no inventário e o que falta.
 * Nunca fabrica nada.
 */
public class CraftingScreen extends Screen {
    private record Entry(RecipeEntry<CraftingRecipe> holder, ItemStack out, String name) {
    }

    private record Req(Ingredient ing, int need) {
    }

    private static final int ROW_H = 20;

    private final Screen parent;
    private final List<Entry> all = new ArrayList<>();
    private List<Entry> view = new ArrayList<>();
    private Entry selected;
    private TextFieldWidget search;
    private boolean onlyCraftable = false;
    private ButtonWidget filterButton;
    private double scroll = 0;
    private int tickCount = 0;
    private int px, py, pw, ph, lx, ly, lw, lh, rx, rw;

    public CraftingScreen(Screen parent) {
        super(Text.literal("Assistente de crafting"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (all.isEmpty() && client != null && client.world != null) loadRecipes();

        pw = Math.min(width - 16, 480);
        ph = Math.min(height - 16, 300);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        lw = Math.max(120, (pw - 24) * 2 / 5);
        lx = px + 8;
        ly = py + 46;
        lh = ph - 46 - 32;
        rx = lx + lw + 8;
        rw = px + pw - 8 - rx;

        String q = search == null ? "" : search.getText();
        search = new TextFieldWidget(textRenderer, lx, py + 24, lw, 16, Text.literal("Buscar"));
        search.setPlaceholder(Text.literal("Buscar item..."));
        search.setText(q);
        search.setChangedListener(s -> refilter());
        addDrawableChild(search);
        setInitialFocus(search);

        filterButton = ButtonWidget.builder(filterLabel(), b -> {
            onlyCraftable = !onlyCraftable;
            b.setMessage(filterLabel());
            refilter();
        }).dimensions(lx, py + ph - 26, lw, 20).build();
        addDrawableChild(filterButton);
        addDrawableChild(ButtonWidget.builder(Text.literal("Fechar"), b -> close())
                .dimensions(rx, py + ph - 26, Math.min(rw, 120), 20).build());
        refilter();
    }

    private Text filterLabel() {
        return Text.literal("Só o que posso fazer: " + (onlyCraftable ? "sim" : "não"));
    }

    private void loadRecipes() {
        var reg = client.world.getRegistryManager();
        for (RecipeEntry<CraftingRecipe> e : client.world.getRecipeManager().listAllOfType(RecipeType.CRAFTING)) {
            CraftingRecipe r = e.value();
            if (r instanceof SpecialCraftingRecipe) continue;
            ItemStack out = r.getResult(reg);
            if (out.isEmpty() || r.getIngredients().isEmpty()) continue;
            all.add(new Entry(e, out, out.getName().getString().toLowerCase(Locale.ROOT)));
        }
        all.sort((a, b) -> a.name().compareTo(b.name()));
    }

    private void refilter() {
        if (search == null) return;
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        ClientPlayerEntity p = client == null ? null : client.player;
        List<Entry> res = new ArrayList<>();
        for (Entry e : all) {
            if (!q.isEmpty() && !e.name().contains(q)) continue;
            if (onlyCraftable && (p == null || !canCraft(e, p))) continue;
            res.add(e);
        }
        view = res;
        scroll = 0;
    }

    // ---------- lógica de materiais ----------

    private static String key(Ingredient ing) {
        String[] ids = Arrays.stream(ing.getMatchingStacks())
                .map(s -> Registries.ITEM.getId(s.getItem()).toString()).sorted().toArray(String[]::new);
        return String.join(",", ids);
    }

    private static List<Req> reqs(CraftingRecipe r) {
        Map<String, Ingredient> ings = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Ingredient ing : r.getIngredients()) {
            if (ing.isEmpty()) continue;
            String k = key(ing);
            ings.putIfAbsent(k, ing);
            counts.merge(k, 1, Integer::sum);
        }
        List<Req> out = new ArrayList<>();
        for (String k : ings.keySet()) out.add(new Req(ings.get(k), counts.get(k)));
        return out;
    }

    private static List<ItemStack> inventory(ClientPlayerEntity p) {
        List<ItemStack> l = new ArrayList<>(p.getInventory().main);
        l.addAll(p.getInventory().offHand);
        return l;
    }

    /** Quanto falta de cada ingrediente, distribuindo o inventário sem usar o mesmo item duas vezes. */
    private static int[] missing(List<Req> reqs, ClientPlayerEntity p) {
        List<ItemStack> inv = inventory(p);
        int[] left = new int[inv.size()];
        for (int i = 0; i < left.length; i++) left[i] = inv.get(i).getCount();
        Integer[] order = new Integer[reqs.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Integer.compare(
                reqs.get(a).ing().getMatchingStacks().length, reqs.get(b).ing().getMatchingStacks().length));
        int[] miss = new int[reqs.size()];
        for (int idx : order) {
            Req r = reqs.get(idx);
            int need = r.need();
            for (int i = 0; i < left.length && need > 0; i++) {
                if (left[i] > 0 && r.ing().test(inv.get(i))) {
                    int take = Math.min(need, left[i]);
                    left[i] -= take;
                    need -= take;
                }
            }
            miss[idx] = need;
        }
        return miss;
    }

    private static int have(Req r, ClientPlayerEntity p) {
        int n = 0;
        for (ItemStack s : inventory(p)) if (!s.isEmpty() && r.ing().test(s)) n += s.getCount();
        return n;
    }

    private static boolean canCraft(Entry e, ClientPlayerEntity p) {
        for (int m : missing(reqs(e.holder().value()), p)) if (m > 0) return false;
        return true;
    }

    private static boolean needsTable(CraftingRecipe r) {
        if (r instanceof ShapedRecipe s) return s.getWidth() > 2 || s.getHeight() > 2;
        return r.getIngredients().size() > 4;
    }

    // ---------- tela ----------

    @Override
    public void tick() {
        if (onlyCraftable && ++tickCount % 20 == 0) refilter();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // desenhado em render()
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xC0000000);
        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, Colors.ACCENT_DARK);
        ctx.fill(px, py, px + pw, py + ph, Colors.PANEL);
        ctx.drawText(textRenderer, Text.literal("ASSISTENTE DE CRAFTING").formatted(Formatting.RED, Formatting.BOLD), px + 8, py + 10, 0xFFFFFFFF, false);
        ctx.fill(px + 8, py + 20, px + pw - 8, py + 21, Colors.ACCENT);

        // lista
        double maxScroll = Math.max(0, view.size() * ROW_H - lh);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        ctx.fill(lx, ly, lx + lw, ly + lh, 0xFF111111);
        ctx.enableScissor(lx, ly, lx + lw, ly + lh);
        for (int i = 0; i < view.size(); i++) {
            int y = ly + i * ROW_H - (int) scroll;
            if (y + ROW_H < ly || y > ly + lh) continue;
            Entry e = view.get(i);
            boolean hov = mx >= lx && mx < lx + lw && my >= y && my < y + ROW_H;
            if (e == selected) ctx.fill(lx, y, lx + lw, y + ROW_H, Colors.ACCENT_DARK);
            else if (hov) ctx.fill(lx, y, lx + lw, y + ROW_H, Colors.ROW_HOVER);
            ctx.drawItem(e.out(), lx + 2, y + 2);
            ctx.drawText(textRenderer, e.out().getName(), lx + 22, y + 6, Colors.TEXT, false);
        }
        ctx.disableScissor();
        if (view.isEmpty()) ctx.drawText(textRenderer, "Nenhuma receita encontrada.", lx + 6, ly + 8, Colors.MUTED, false);

        renderDetails(ctx);
        super.render(ctx, mx, my, delta);
    }

    private void renderDetails(DrawContext ctx) {
        ClientPlayerEntity p = client.player;
        if (selected == null || p == null) {
            ctx.drawText(textRenderer, "Selecione uma receita à esquerda.", rx, ly + 6, Colors.MUTED, false);
            return;
        }
        CraftingRecipe r = selected.holder().value();
        ctx.drawItem(selected.out(), rx, ly);
        ctx.drawText(textRenderer, Text.literal(selected.out().getCount() + "x ").append(selected.out().getName()), rx + 22, ly + 4, 0xFFFFFFFF, false);
        ctx.drawText(textRenderer, needsTable(r) ? "Precisa de mesa de trabalho (3x3)" : "Dá para fazer no inventário (2x2)", rx, ly + 24, Colors.MUTED, false);

        List<Req> reqs = reqs(r);
        int[] miss = missing(reqs, p);
        int y = ly + 40;
        long t = System.currentTimeMillis() / 1000;
        boolean anyMissing = false;
        for (int i = 0; i < reqs.size(); i++) {
            Req q = reqs.get(i);
            ItemStack[] ms = q.ing().getMatchingStacks();
            ItemStack icon = ms.length == 0 ? ItemStack.EMPTY : ms[(int) (t % ms.length)];
            boolean ok = miss[i] == 0;
            if (!ok) anyMissing = true;
            ctx.drawItem(icon, rx, y);
            String nm = icon.isEmpty() ? "?" : icon.getName().getString() + (ms.length > 1 ? " (ou similar)" : "");
            ctx.drawText(textRenderer, nm, rx + 20, y + 1, Colors.TEXT, false);
            ctx.drawText(textRenderer, "tem " + have(q, p) + " / precisa " + q.need(), rx + 20, y + 10, ok ? Colors.GOOD : Colors.BAD, false);
            y += 20;
            if (y > ly + lh - 24) break;
        }
        if (anyMissing) {
            StringBuilder sb = new StringBuilder("Faltam: ");
            boolean first = true;
            for (int i = 0; i < reqs.size(); i++) {
                if (miss[i] <= 0) continue;
                ItemStack[] ms = reqs.get(i).ing().getMatchingStacks();
                if (!first) sb.append(", ");
                sb.append(miss[i]).append("x ").append(ms.length == 0 ? "?" : ms[0].getName().getString());
                first = false;
            }
            var lines = textRenderer.wrapLines(Text.literal(sb.toString()), rw);
            int ly2 = py + ph - 32 - lines.size() * 10;
            for (var l : lines) {
                ctx.drawText(textRenderer, l, rx, ly2, Colors.BAD, false);
                ly2 += 10;
            }
        } else {
            ctx.drawText(textRenderer, "Você tem todos os materiais.", rx, py + ph - 42, Colors.GOOD, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0 && mx >= lx && mx < lx + lw && my >= ly && my < ly + lh) {
            int idx = (int) ((my - ly + scroll) / ROW_H);
            if (idx >= 0 && idx < view.size()) selected = view.get(idx);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll -= v * ROW_H;
        return true;
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
