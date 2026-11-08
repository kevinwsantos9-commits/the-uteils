package dev.nivek.utility.gui;

import dev.nivek.utility.util.Colors;
import dev.nivek.utility.waypoint.Waypoint;
import dev.nivek.utility.waypoint.WaypointManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/** Criar ou editar um waypoint (nome, coordenadas e tipo). */
public class WaypointEditScreen extends Screen {
    private final Screen parent;
    private final Waypoint editing;
    private TextFieldWidget name, fx, fy, fz;
    private int type;
    private ButtonWidget typeButton;
    private String error = "";
    private int px, py, pw, ph;

    public WaypointEditScreen(Screen parent, Waypoint editing) {
        super(Text.literal(editing == null ? "Novo waypoint" : "Editar waypoint"));
        this.parent = parent;
        this.editing = editing;
        this.type = editing == null ? 0 : editing.type;
    }

    @Override
    protected void init() {
        pw = 260;
        ph = 170;
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        BlockPos bp = client.player != null ? client.player.getBlockPos() : BlockPos.ORIGIN;

        name = new TextFieldWidget(textRenderer, px + 60, py + 30, pw - 70, 16, Text.literal("Nome"));
        name.setMaxLength(32);
        name.setText(editing == null ? "Novo ponto" : editing.name);
        fx = coord(px + 60, py + 54, editing == null ? bp.getX() : editing.x);
        fy = coord(px + 128, py + 54, editing == null ? bp.getY() : editing.y);
        fz = coord(px + 196, py + 54, editing == null ? bp.getZ() : editing.z);
        addDrawableChild(name);
        addDrawableChild(fx);
        addDrawableChild(fy);
        addDrawableChild(fz);

        typeButton = ButtonWidget.builder(Text.literal(Waypoint.TYPES[type]), b -> {
            type = (type + 1) % Waypoint.TYPES.length;
            b.setMessage(Text.literal(Waypoint.TYPES[type]));
        }).dimensions(px + 60, py + 78, pw - 70, 18).build();
        addDrawableChild(typeButton);

        addDrawableChild(ButtonWidget.builder(Text.literal("Salvar"), b -> save())
                .dimensions(px + 10, py + ph - 28, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancelar"), b -> close())
                .dimensions(px + pw - 128, py + ph - 28, 118, 20).build());
        setInitialFocus(name);
    }

    private TextFieldWidget coord(int x, int y, int value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, 60, 16, Text.literal("coord"));
        f.setMaxLength(9);
        f.setText(String.valueOf(value));
        return f;
    }

    private void save() {
        try {
            String n = name.getText().trim();
            if (n.isEmpty()) {
                error = "Dê um nome ao waypoint.";
                return;
            }
            int x = Integer.parseInt(fx.getText().trim());
            int y = Integer.parseInt(fy.getText().trim());
            int z = Integer.parseInt(fz.getText().trim());
            if (editing == null) {
                WaypointManager.create(n, x, y, z, type);
            } else {
                editing.name = n;
                editing.x = x;
                editing.y = y;
                editing.z = z;
                editing.type = type;
                WaypointManager.save();
            }
            close();
        } catch (NumberFormatException e) {
            error = "Coordenadas devem ser números inteiros.";
        }
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
        ctx.drawText(textRenderer, Text.literal(title.getString().toUpperCase()).formatted(Formatting.RED, Formatting.BOLD), px + 10, py + 10, 0xFFFFFFFF, false);
        ctx.drawText(textRenderer, "Nome", px + 10, py + 34, Colors.TEXT, false);
        ctx.drawText(textRenderer, "X / Y / Z", px + 10, py + 58, Colors.TEXT, false);
        ctx.drawText(textRenderer, "Tipo", px + 10, py + 83, Colors.TEXT, false);
        if (!error.isEmpty()) ctx.drawText(textRenderer, error, px + 10, py + 108, Colors.BAD, false);
        super.render(ctx, mx, my, delta);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
