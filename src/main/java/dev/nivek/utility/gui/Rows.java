package dev.nivek.utility.gui;

import dev.nivek.utility.config.ConfigManager;
import dev.nivek.utility.util.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Linhas de configuração da tela principal. Todas ligadas a uma ação real (getter/setter/Runnable). */
public final class Rows {
    private Rows() {
    }

    public abstract static class Row {
        public int height = 22;
        int bx, by, bw;

        protected void place(int x, int y, int w) {
            bx = x;
            by = y;
            bw = w;
        }

        protected boolean hovered(int mx, int my) {
            return mx >= bx && mx < bx + bw && my >= by && my < by + height;
        }

        public abstract void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my);

        public boolean click(double mx, double my, int button) {
            return false;
        }

        public void drag(double mx, double my) {
        }

        public void release() {
        }

        public boolean key(int keyCode, int scanCode) {
            return false;
        }

        public boolean listening() {
            return false;
        }

        public void cancelListen() {
        }
    }

    public static class Section extends Row {
        private final String title;

        public Section(String title) {
            this.title = title;
            this.height = 18;
        }

        @Override
        public void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my) {
            place(x, y, w);
            ctx.drawText(tr, title.toUpperCase(), x + 2, y + 8, Colors.ACCENT, false);
            ctx.fill(x, y + 17, x + w, y + 18, 0xFF333333);
        }
    }

    public static class Label extends Row {
        private final List<OrderedText> lines;

        public Label(String text, int width) {
            this.lines = MinecraftClient.getInstance().textRenderer.wrapLines(Text.literal(text), Math.max(80, width - 8));
            this.height = Math.max(14, lines.size() * 10 + 6);
        }

        @Override
        public void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my) {
            place(x, y, w);
            int ly = y + 3;
            for (OrderedText l : lines) {
                ctx.drawText(tr, l, x + 4, ly, Colors.MUTED, false);
                ly += 10;
            }
        }
    }

    public static class Toggle extends Row {
        private final String label;
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;
        private float anim = -1f, hov = 0f;

        public Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
            this.label = label;
            this.get = get;
            this.set = set;
        }

        @Override
        public void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my) {
            place(x, y, w);
            boolean v = get.getAsBoolean();
            if (anim < 0) anim = v ? 1f : 0f;
            anim = Ui.approach(anim, v ? 1f : 0f);
            hov = Ui.approach(hov, hovered(mx, my) ? 1f : 0f);
            ctx.fill(x, y, x + w, y + height, Colors.lerp(Colors.ROW, Colors.ROW_HOVER, hov));
            ctx.drawText(tr, label, x + 8, y + 7, Colors.TEXT, false);
            int tx = x + w - 38, ty = y + 5;
            ctx.fill(tx, ty, tx + 28, ty + 12, Colors.lerp(0xFF444444, Colors.ACCENT, anim));
            int kx = tx + 2 + (int) (anim * 14);
            ctx.fill(kx, ty + 2, kx + 10, ty + 10, 0xFFFFFFFF);
        }

        @Override
        public boolean click(double mx, double my, int button) {
            if (button == 0 && hovered((int) mx, (int) my)) {
                set.accept(!get.getAsBoolean());
                ConfigManager.save();
                return true;
            }
            return false;
        }
    }

    public static class Slider extends Row {
        private final String label, suffix;
        private final int min, max;
        private final IntSupplier get;
        private final IntConsumer set;
        private boolean dragging = false;
        private float hov = 0f;

        public Slider(String label, int min, int max, String suffix, IntSupplier get, IntConsumer set) {
            this.label = label;
            this.min = min;
            this.max = max;
            this.suffix = suffix;
            this.get = get;
            this.set = set;
        }

        private int trackX() {
            return bx + bw - 176;
        }

        private static final int TRACK_W = 110;

        @Override
        public void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my) {
            place(x, y, w);
            hov = Ui.approach(hov, (hovered(mx, my) || dragging) ? 1f : 0f);
            ctx.fill(x, y, x + w, y + height, Colors.lerp(Colors.ROW, Colors.ROW_HOVER, hov));
            ctx.drawText(tr, label, x + 8, y + 7, Colors.TEXT, false);
            int tx = trackX();
            float t = (get.getAsInt() - min) / (float) Math.max(1, max - min);
            ctx.fill(tx, y + 9, tx + TRACK_W, y + 13, 0xFF444444);
            ctx.fill(tx, y + 9, tx + (int) (t * TRACK_W), y + 13, Colors.ACCENT);
            ctx.fill(tx + (int) (t * TRACK_W) - 2, y + 5, tx + (int) (t * TRACK_W) + 2, y + 17, 0xFFFFFFFF);
            ctx.drawText(tr, get.getAsInt() + suffix, tx + TRACK_W + 8, y + 7, Colors.TEXT, false);
        }

        private void update(double mx) {
            float t = (float) Math.max(0, Math.min(1, (mx - trackX()) / TRACK_W));
            set.accept(min + Math.round(t * (max - min)));
        }

        @Override
        public boolean click(double mx, double my, int button) {
            if (button == 0 && hovered((int) mx, (int) my) && mx >= trackX() - 6 && mx <= trackX() + TRACK_W + 6) {
                dragging = true;
                update(mx);
                return true;
            }
            return false;
        }

        @Override
        public void drag(double mx, double my) {
            if (dragging) update(mx);
        }

        @Override
        public void release() {
            if (dragging) {
                dragging = false;
                ConfigManager.save();
            }
        }
    }

    public static class Cycle extends Row {
        private final String label;
        private final String[] options;
        private final IntSupplier get;
        private final IntConsumer set;
        private float hov = 0f;

        public Cycle(String label, String[] options, IntSupplier get, IntConsumer set) {
            this.label = label;
            this.options = options;
            this.get = get;
            this.set = set;
        }

        @Override
        public void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my) {
            place(x, y, w);
            hov = Ui.approach(hov, hovered(mx, my) ? 1f : 0f);
            ctx.fill(x, y, x + w, y + height, Colors.lerp(Colors.ROW, Colors.ROW_HOVER, hov));
            ctx.drawText(tr, label, x + 8, y + 7, Colors.TEXT, false);
            String v = options[Math.floorMod(get.getAsInt(), options.length)];
            int pw = tr.getWidth(v) + 12;
            ctx.fill(x + w - pw - 8, y + 3, x + w - 8, y + 19, Colors.lerp(Colors.ACCENT_DARK, Colors.ACCENT, hov));
            ctx.drawText(tr, v, x + w - pw - 2, y + 7, 0xFFFFFFFF, false);
        }

        @Override
        public boolean click(double mx, double my, int button) {
            if (hovered((int) mx, (int) my) && (button == 0 || button == 1)) {
                int d = button == 0 ? 1 : -1;
                set.accept(Math.floorMod(get.getAsInt() + d, options.length));
                ConfigManager.save();
                return true;
            }
            return false;
        }
    }

    public static class ButtonRow extends Row {
        private final Supplier<String> label;
        private final Runnable action;
        private float hov = 0f;

        public ButtonRow(Supplier<String> label, Runnable action) {
            this.label = label;
            this.action = action;
        }

        public ButtonRow(String label, Runnable action) {
            this(() -> label, action);
        }

        @Override
        public void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my) {
            place(x, y, w);
            hov = Ui.approach(hov, hovered(mx, my) ? 1f : 0f);
            ctx.fill(x, y, x + w, y + height, Colors.lerp(Colors.ACCENT_DARK, Colors.ACCENT, hov));
            String s = label.get();
            ctx.drawText(tr, s, x + (w - tr.getWidth(s)) / 2, y + 7, 0xFFFFFFFF, false);
        }

        @Override
        public boolean click(double mx, double my, int button) {
            if (button == 0 && hovered((int) mx, (int) my)) {
                action.run();
                return true;
            }
            return false;
        }
    }

    public static class KeyRow extends Row {
        private final KeyBinding kb;
        private boolean listening = false;
        private float hov = 0f;

        public KeyRow(KeyBinding kb) {
            this.kb = kb;
        }

        @Override
        public void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my) {
            place(x, y, w);
            hov = Ui.approach(hov, hovered(mx, my) ? 1f : 0f);
            ctx.fill(x, y, x + w, y + height, Colors.lerp(Colors.ROW, Colors.ROW_HOVER, hov));
            ctx.drawText(tr, I18n.translate(kb.getTranslationKey()), x + 8, y + 7, Colors.TEXT, false);
            String v = listening ? "> pressione uma tecla <" : kb.getBoundKeyLocalizedText().getString();
            int pw = Math.max(60, tr.getWidth(v) + 12);
            ctx.fill(x + w - pw - 8, y + 3, x + w - 8, y + 19, listening ? Colors.ACCENT : 0xFF3A3A3A);
            ctx.drawText(tr, v, x + w - pw - 8 + (pw - tr.getWidth(v)) / 2, y + 7, 0xFFFFFFFF, false);
        }

        @Override
        public boolean click(double mx, double my, int button) {
            if (button == 0 && hovered((int) mx, (int) my)) {
                listening = true;
                return true;
            }
            return false;
        }

        @Override
        public boolean listening() {
            return listening;
        }

        @Override
        public void cancelListen() {
            listening = false;
        }

        @Override
        public boolean key(int keyCode, int scanCode) {
            if (!listening) return false;
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listening = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                kb.setBoundKey(InputUtil.UNKNOWN_KEY);
            } else {
                kb.setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
            }
            KeyBinding.updateKeysByCode();
            MinecraftClient.getInstance().options.write();
            listening = false;
            return true;
        }
    }
}
