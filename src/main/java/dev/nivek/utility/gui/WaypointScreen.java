package dev.nivek.utility.gui;

import dev.nivek.utility.config.ConfigManager;
import dev.nivek.utility.util.Colors;
import dev.nivek.utility.waypoint.Waypoint;
import dev.nivek.utility.waypoint.WaypointManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Gerenciador de waypoints: criar, editar/renomear, excluir e escolher o que aparece no HUD. */
public class WaypointScreen extends Screen {
    private static final Item[] ICONS = {Items.RED_BED, Items.EMERALD, Items.IRON_PICKAXE, Items.ENDER_EYE, Items.OBSIDIAN, Items.COMPASS};
    private static final int ROW_H = 22;

    private final Screen parent;
    private int px, py, pw, ph, lx, ly, lw, lh;
    private int selected = -1;
    private double scroll = 0;

    public WaypointScreen(Screen parent) {
        super(Text.literal("Waypoints"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        pw = Math.min(width - 16, 440);
        ph = Math.min(height - 16, 290);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        lx = px + 8;
        ly = py + 28;
        lw = pw - 16;
        lh = ph - 28 - 34;

        int bw = (lw - 16) / 5;
        int by = py + ph - 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("Novo"), b ->
                client.setScreen(new WaypointEditScreen(this, null))).dimensions(lx, by, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Editar"), b -> {
            Waypoint w = current();
            if (w != null) client.setScreen(new WaypointEditScreen(this, w));
        }).dimensions(lx + (bw + 4), by, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Excluir"), b -> {
            Waypoint w = current();
            if (w != null) {
                WaypointManager.remove(w);
                selected = -1;
            }
        }).dimensions(lx + (bw + 4) * 2, by, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Mostrar no HUD"), b -> {
            Waypoint w = current();
            if (w != null) {
                ConfigManager.get().selectedWaypoint = w.id.equals(ConfigManager.get().selectedWaypoint) ? "" : w.id;
                ConfigManager.save();
            }
        }).dimensions(lx + (bw + 4) * 3, by, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Fechar"), b -> close())
                .dimensions(lx + (bw + 4) * 4, by, bw, 20).build());
    }

    private Waypoint current() {
        List<Waypoint> list = WaypointManager.list();
        return selected >= 0 && selected < list.size() ? list.get(selected) : null;
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
        ctx.drawText(textRenderer, Text.literal("WAYPOINTS").formatted(Formatting.RED, Formatting.BOLD), px + 8, py + 10, 0xFFFFFFFF, false);
        ctx.fill(lx, py + 24, lx + lw, py + 25, Colors.ACCENT);

        List<Waypoint> list = WaypointManager.list();
        String dim = WaypointManager.currentDimension();
        double maxScroll = Math.max(0, list.size() * ROW_H - lh);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        ctx.enableScissor(lx, ly, lx + lw, ly + lh);
        if (list.isEmpty()) {
            ctx.drawText(textRenderer, "Nenhum waypoint neste mundo. Clique em Novo.", lx + 6, ly + 8, Colors.MUTED, false);
        }
        for (int i = 0; i < list.size(); i++) {
            Waypoint w = list.get(i);
            int y = ly + i * ROW_H - (int) scroll;
            if (y + ROW_H < ly || y > ly + lh) continue;
            boolean hov = mx >= lx && mx < lx + lw && my >= y && my < y + ROW_H - 2;
            ctx.fill(lx, y, lx + lw, y + ROW_H - 2, i == selected ? Colors.ACCENT_DARK : (hov ? Colors.ROW_HOVER : Colors.ROW));
            ctx.drawItem(new ItemStack(ICONS[Math.max(0, Math.min(ICONS.length - 1, w.type))]), lx + 3, y + 1);
            boolean onHud = w.id.equals(ConfigManager.get().selectedWaypoint);
            ctx.drawText(textRenderer, (onHud ? "★ " : "") + w.name + "  (" + w.typeName() + ")", lx + 24, y + 3, Colors.TEXT, false);
            String coords = w.x + " " + w.y + " " + w.z;
            String info;
            if (w.dimension != null && !w.dimension.equals(dim)) {
                info = "outra dimensão";
            } else if (client.player != null) {
                info = Math.round(w.distanceTo(client.player.getX(), client.player.getY(), client.player.getZ())) + " m";
            } else {
                info = "";
            }
            ctx.drawText(textRenderer, coords + "   " + info, lx + 24, y + 12, Colors.MUTED, false);
        }
        ctx.disableScissor();
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0 && mx >= lx && mx < lx + lw && my >= ly && my < ly + lh) {
            int idx = (int) ((my - ly + scroll) / ROW_H);
            selected = idx >= 0 && idx < WaypointManager.list().size() ? idx : -1;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll -= v * 20;
        return true;
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
