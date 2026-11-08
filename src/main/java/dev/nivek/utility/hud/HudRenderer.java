package dev.nivek.utility.hud;

import dev.nivek.utility.config.ConfigManager;
import dev.nivek.utility.config.NivekConfig;
import dev.nivek.utility.util.Colors;
import dev.nivek.utility.waypoint.Waypoint;
import dev.nivek.utility.waypoint.WaypointManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public final class HudRenderer {
    public static final int LINE_H = 11;

    public record Line(NivekConfig.HudEntry entry, Text text) {
    }

    private HudRenderer() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            NivekConfig c = ConfigManager.get();
            if (!c.hudEnabled || mc.player == null || mc.world == null || mc.options.hudHidden) return;
            draw(ctx, mc, collect(mc, false));
        });
    }

    public static float scale() {
        return ConfigManager.get().hudScale / 100f;
    }

    public static int width(TextRenderer tr, Line l) {
        return tr.getWidth(l.text()) + 8;
    }

    public static void draw(DrawContext ctx, MinecraftClient mc, List<Line> lines) {
        float s = scale();
        TextRenderer tr = mc.textRenderer;
        ctx.getMatrices().push();
        ctx.getMatrices().scale(s, s, 1f);
        for (Line l : lines) {
            int x = l.entry().x, y = l.entry().y;
            ctx.fill(x, y, x + width(tr, l), y + LINE_H, 0x99000000);
            ctx.fill(x, y, x + 2, y + LINE_H, Colors.ACCENT);
            ctx.drawText(tr, l.text(), x + 5, y + 2, 0xFFFFFFFF, false);
        }
        ctx.getMatrices().pop();
    }

    /** Monta as linhas visíveis. Em modo preview (editor) mostra exemplos para elementos sem dado no momento. */
    public static List<Line> collect(MinecraftClient mc, boolean preview) {
        NivekConfig c = ConfigManager.get();
        List<Line> out = new ArrayList<>();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return out;
        BlockPos bp = p.getBlockPos();

        if (c.hudCoords.enabled) {
            out.add(new Line(c.hudCoords, Text.literal("XYZ: " + bp.getX() + " " + bp.getY() + " " + bp.getZ())));
        }
        if (c.hudBiome.enabled) {
            String key = mc.world.getBiome(bp).getKey().map(k -> k.getValue().toTranslationKey("biome")).orElse("?");
            out.add(new Line(c.hudBiome, Text.literal("Bioma: ").append(Text.translatable(key))));
        }
        if (c.hudTool.enabled) {
            ItemStack hand = p.getMainHandStack();
            if (hand.isDamageable()) {
                out.add(new Line(c.hudTool, durability("Ferramenta: ", hand.getMaxDamage() - hand.getDamage(), hand.getMaxDamage())));
            } else if (preview) {
                out.add(new Line(c.hudTool, durability("Ferramenta: ", 1200, 1561)));
            }
        }
        if (c.hudArmor.enabled) {
            int cur = 0, max = 0;
            for (ItemStack a : p.getInventory().armor) {
                if (!a.isEmpty() && a.isDamageable()) {
                    cur += a.getMaxDamage() - a.getDamage();
                    max += a.getMaxDamage();
                }
            }
            if (max > 0) out.add(new Line(c.hudArmor, durability("Armadura: ", cur, max)));
            else if (preview) out.add(new Line(c.hudArmor, durability("Armadura: ", 900, 1200)));
        }
        if (c.hudFood.enabled) {
            int f = p.getHungerManager().getFoodLevel();
            out.add(new Line(c.hudFood, colored("Comida: " + f + "/20", f / 20f)));
        }
        if (c.hudHealth.enabled) {
            float hp = p.getHealth();
            String extra = p.getAbsorptionAmount() > 0 ? " +" + (int) Math.ceil(p.getAbsorptionAmount()) : "";
            out.add(new Line(c.hudHealth, colored("Vida: " + (int) Math.ceil(hp) + "/" + (int) Math.ceil(p.getMaxHealth()) + extra,
                    hp / Math.max(1f, p.getMaxHealth()))));
        }
        if (c.hudWaypoint.enabled) {
            Waypoint w = WaypointManager.selected();
            if (w != null) {
                out.add(new Line(c.hudWaypoint, waypointText(p, w)));
            } else if (preview) {
                out.add(new Line(c.hudWaypoint, Text.literal("Casa: 128 m ↗")));
            }
        }
        if (c.hudFps.enabled) {
            out.add(new Line(c.hudFps, Text.literal("FPS: " + mc.getCurrentFps())));
        }
        if (c.hudClock.enabled) {
            long t = mc.world.getTimeOfDay() % 24000L;
            int hours = (int) ((t / 1000 + 6) % 24);
            int mins = (int) ((t % 1000) * 60 / 1000);
            out.add(new Line(c.hudClock, Text.literal(String.format("Hora: %02d:%02d", hours, mins))));
        }
        return out;
    }

    private static MutableText colored(String s, float fraction) {
        int rgb = Colors.health(fraction) & 0xFFFFFF;
        return Text.literal(s).styled(st -> st.withColor(rgb));
    }

    private static MutableText durability(String label, int cur, int max) {
        float f = max <= 0 ? 0f : (float) cur / max;
        return colored(label + cur + "/" + max + " (" + Math.round(f * 100) + "%)", f);
    }

    private static Text waypointText(ClientPlayerEntity p, Waypoint w) {
        String dim = MinecraftClient.getInstance().world.getRegistryKey().getValue().toString();
        if (w.dimension != null && !w.dimension.equals(dim)) {
            return Text.literal(w.name + ": outra dimensão");
        }
        double dist = w.distanceTo(p.getX(), p.getY(), p.getZ());
        double dx = w.x + 0.5 - p.getX(), dz = w.z + 0.5 - p.getZ();
        float bearing = (float) (MathHelper.atan2(-dx, dz) * (180.0 / Math.PI));
        float rel = MathHelper.wrapDegrees(bearing - p.getYaw());
        String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        int idx = Math.floorMod(Math.round(rel / 45f), 8);
        String d = dist >= 1000 ? String.format("%.1f km", dist / 1000.0) : Math.round(dist) + " m";
        return Text.literal(w.name + ": " + d + " " + arrows[idx]);
    }
}
