package dev.nivek.utility.gui;

import dev.nivek.utility.config.ConfigManager;
import dev.nivek.utility.hud.HudRenderer;
import dev.nivek.utility.util.Colors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

/** Arraste cada elemento do HUD com o mouse. Para esconder/mostrar use a categoria HUD. */
public class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudRenderer.Line dragging;
    private double offX, offY;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Editor de HUD"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x66000000);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        List<HudRenderer.Line> lines = HudRenderer.collect(client, true);
        HudRenderer.draw(ctx, client, lines);
        float s = HudRenderer.scale();
        for (HudRenderer.Line l : lines) {
            int x = (int) (l.entry().x * s), y = (int) (l.entry().y * s);
            int w = (int) (HudRenderer.width(textRenderer, l) * s), h = (int) (HudRenderer.LINE_H * s);
            boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
            int col = (l == dragging || hov) ? 0xFFFFFFFF : Colors.ACCENT;
            ctx.drawBorder(x - 1, y - 1, w + 2, h + 2, col);
        }
        ctx.drawCenteredTextWithShadow(textRenderer, "Arraste os elementos para mover. ESC para sair.", width / 2, height - 20, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            float s = HudRenderer.scale();
            List<HudRenderer.Line> lines = HudRenderer.collect(client, true);
            for (int i = lines.size() - 1; i >= 0; i--) {
                HudRenderer.Line l = lines.get(i);
                double x = l.entry().x * s, y = l.entry().y * s;
                if (mx >= x && mx < x + HudRenderer.width(textRenderer, l) * s && my >= y && my < y + HudRenderer.LINE_H * s) {
                    dragging = l;
                    offX = mx / s - l.entry().x;
                    offY = my / s - l.entry().y;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            float s = HudRenderer.scale();
            int maxX = (int) (width / s) - 10, maxY = (int) (height / s) - HudRenderer.LINE_H;
            dragging.entry().x = (int) Math.max(0, Math.min(maxX, mx / s - offX));
            dragging.entry().y = (int) Math.max(0, Math.min(maxY, my / s - offY));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) {
            dragging = null;
            ConfigManager.save();
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void close() {
        ConfigManager.save();
        if (client != null) client.setScreen(parent);
    }
}
