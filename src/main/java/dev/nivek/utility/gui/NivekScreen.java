package dev.nivek.utility.gui;

import dev.nivek.utility.config.ConfigManager;
import dev.nivek.utility.config.NivekConfig;
import dev.nivek.utility.gui.Rows.*;
import dev.nivek.utility.keybind.Keybinds;
import dev.nivek.utility.network.OrganizePayload;
import dev.nivek.utility.util.Colors;
import dev.nivek.utility.waypoint.Waypoint;
import dev.nivek.utility.waypoint.WaypointManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Tela principal NIVEK UTILITY: sidebar com 10 categorias e painel de opções rolável. */
public class NivekScreen extends Screen {
    private static final String[] CATS = {"Geral", "Árvores", "Mineração", "Inventário", "Agricultura",
            "Waypoints", "HUD", "Tochas", "Crafting", "Controles"};
    private static final Item[] ICONS = {Items.NETHER_STAR, Items.IRON_AXE, Items.DIAMOND_PICKAXE, Items.CHEST,
            Items.WHEAT, Items.COMPASS, Items.CLOCK, Items.TORCH, Items.CRAFTING_TABLE, Items.LEVER};
    private static final int SIDE_W = 106;

    private final Screen parent;
    private int cat = 0;
    private final List<Row> rows = new ArrayList<>();
    private double scroll = 0;
    private int px, py, pw, ph, cx, cy, cw, ch;
    private float barY = -1f;
    private final float[] catHover = new float[CATS.length];
    private boolean confirmReset = false;

    public NivekScreen(Screen parent) {
        super(Text.literal("NIVEK UTILITY"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        pw = Math.min(width - 16, 480);
        ph = Math.min(height - 16, 300);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        cx = px + SIDE_W + 10;
        cy = py + 30;
        cw = pw - SIDE_W - 20;
        ch = ph - 38;
        buildRows();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // fundo desenhado em render()
    }

    private void setCategory(int i) {
        cat = i;
        scroll = 0;
        confirmReset = false;
        buildRows();
    }

    private int catH() {
        return Math.max(14, Math.min(20, (ph - 44) / CATS.length));
    }

    private void buildRows() {
        rows.clear();
        NivekConfig c = ConfigManager.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        switch (cat) {
            case 0 -> {
                rows.add(new Section("Sobre"));
                rows.add(new Label("Ferramentas de qualidade de vida para Survival. Os recursos de quebra, colheita, tochas e "
                        + "organização rodam no servidor, que aplica os próprios limites e proteções.", cw));
                rows.add(new Section("Pesca e alimentação"));
                rows.add(new Toggle("Alerta de peixe fisgado", () -> c.fishAlert, v -> c.fishAlert = v));
                rows.add(new Toggle("Som no alerta de pesca", () -> c.fishSound, v -> c.fishSound = v));
                rows.add(new Toggle("Aviso de pouca comida", () -> c.lowFoodAlert, v -> c.lowFoodAlert = v));
                rows.add(new Slider("Avisar quando a fome for ≤", 1, 19, "", () -> c.lowFoodThreshold, v -> c.lowFoodThreshold = v));
                rows.add(new ButtonRow(() -> "Alimento favorito: " + favoriteName() + "  (clique: usar item da mão / limpar)", this::toggleFavorite));
                rows.add(new Label("A tecla Comer usa o alimento favorito na hotbar; sem favorito, usa o item da mão se for comida, "
                        + "ou o primeiro alimento da hotbar. Nada é consumido sem você apertar a tecla.", cw));
                rows.add(new Section("Configurações"));
                rows.add(new ButtonRow(() -> confirmReset ? "Clique novamente para CONFIRMAR" : "Restaurar configurações padrão", () -> {
                    if (!confirmReset) {
                        confirmReset = true;
                    } else {
                        ConfigManager.reset();
                        Keybinds.resetAll();
                        confirmReset = false;
                        buildRows();
                    }
                }));
            }
            case 1 -> {
                rows.add(new Section("Tree Capitator"));
                rows.add(new Toggle("Ativar Tree Capitator", () -> c.treeEnabled, v -> c.treeEnabled = v));
                rows.add(new Slider("Limite de blocos", 1, 512, "", () -> c.treeMax, v -> c.treeMax = v));
                rows.add(new Toggle("Só quebrar pela base da árvore", () -> c.treeBaseOnly, v -> c.treeBaseOnly = v));
                rows.add(new Label("Funciona com machado, em árvores com folhas. Gasta durabilidade por tronco e para antes "
                        + "de quebrar a ferramenta. Agache para quebrar só 1 tronco. O servidor pode limitar o máximo.", cw));
            }
            case 2 -> {
                rows.add(new Section("Vein Miner"));
                rows.add(new Toggle("Ativar Vein Miner", () -> c.veinEnabled, v -> c.veinEnabled = v));
                rows.add(new Slider("Limite de blocos", 1, 512, "", () -> c.veinMax, v -> c.veinMax = v));
                rows.add(new Cycle("Ativação", new String[]{"Sempre", "Agachado", "Segurar tecla"}, () -> c.veinMode, v -> c.veinMode = v));
                rows.add(new Label("Exige a ferramenta correta para o minério. Funciona com minérios de outros mods que usem a "
                        + "tag c:ores ou terminem em _ore. Tecla configurável em Controles.", cw));
                rows.add(new Section("Mineração 3x3"));
                rows.add(new Toggle("Ativar mineração 3x3", () -> c.areaEnabled, v -> c.areaEnabled = v));
                rows.add(new Label("Picaretas e pás quebram um plano 3x3 na face batida. Não quebra bedrock, blocos com inventário "
                        + "nem blocos protegidos. Agache para quebrar só 1 bloco.", cw));
            }
            case 3 -> {
                rows.add(new Section("Organizador"));
                rows.add(new Toggle("Mostrar botões nas telas de inventário e baú", () -> c.showInvButtons, v -> c.showInvButtons = v));
                rows.add(new ButtonRow("Organizar meu inventário agora", () -> {
                    if (ClientPlayNetworking.canSend(OrganizePayload.ID)) ClientPlayNetworking.send(new OrganizePayload(0));
                }));
                rows.add(new Label("Ordem: ferramentas, armaduras, comida, blocos, recursos. Junta pilhas iguais, nunca descarta itens "
                        + "e não mexe na hotbar. Em baús, barris e shulkers aparecem os botões Organizar baú e Mover itens iguais "
                        + "(manda para o baú só o que ele já contém).", cw));
            }
            case 4 -> {
                rows.add(new Section("Agricultura"));
                rows.add(new Toggle("Colher e replantar com clique direito", () -> c.farmClick, v -> c.farmClick = v));
                rows.add(new Toggle("Modo automático (em volta do jogador)", () -> c.farmAuto, v -> c.farmAuto = v));
                rows.add(new Slider("Raio do modo automático", 1, 5, " blocos", () -> c.farmRadius, v -> c.farmRadius = v));
                rows.add(new Label("Só colhe plantações maduras (trigo, cenoura, batata, beterraba e plantações de mods que usem CropBlock). "
                        + "Replanta usando uma semente dos drops ou do inventário.", cw));
            }
            case 5 -> {
                rows.add(new Section("Waypoints"));
                rows.add(new ButtonRow("Abrir gerenciador de waypoints", () -> mc.setScreen(new WaypointScreen(this))));
                rows.add(new ButtonRow("Criar waypoint \"Casa\" na minha posição", () -> {
                    if (mc.player != null) {
                        var bp = mc.player.getBlockPos();
                        Waypoint w = WaypointManager.create("Casa", bp.getX(), bp.getY(), bp.getZ(), 0);
                        c.selectedWaypoint = w.id;
                        ConfigManager.save();
                    }
                }));
                rows.add(new Label("O waypoint selecionado aparece no HUD com distância e direção. Os pontos são salvos por mundo/servidor.", cw));
            }
            case 6 -> {
                rows.add(new Section("HUD"));
                rows.add(new Toggle("Mostrar HUD", () -> c.hudEnabled, v -> c.hudEnabled = v));
                rows.add(new Slider("Tamanho do HUD", 50, 200, "%", () -> c.hudScale, v -> c.hudScale = v));
                rows.add(new ButtonRow("Mover elementos (arraste com o mouse)", () -> mc.setScreen(new HudEditorScreen(this))));
                rows.add(new Section("Elementos"));
                rows.add(new Toggle("Coordenadas XYZ", () -> c.hudCoords.enabled, v -> c.hudCoords.enabled = v));
                rows.add(new Toggle("Bioma", () -> c.hudBiome.enabled, v -> c.hudBiome.enabled = v));
                rows.add(new Toggle("Durabilidade da ferramenta", () -> c.hudTool.enabled, v -> c.hudTool.enabled = v));
                rows.add(new Toggle("Durabilidade da armadura", () -> c.hudArmor.enabled, v -> c.hudArmor.enabled = v));
                rows.add(new Toggle("Comida", () -> c.hudFood.enabled, v -> c.hudFood.enabled = v));
                rows.add(new Toggle("Vida", () -> c.hudHealth.enabled, v -> c.hudHealth.enabled = v));
                rows.add(new Toggle("Distância até o waypoint", () -> c.hudWaypoint.enabled, v -> c.hudWaypoint.enabled = v));
                rows.add(new Toggle("FPS", () -> c.hudFps.enabled, v -> c.hudFps.enabled = v));
                rows.add(new Toggle("Relógio do mundo", () -> c.hudClock.enabled, v -> c.hudClock.enabled = v));
            }
            case 7 -> {
                rows.add(new Section("Tochas inteligentes"));
                rows.add(new Toggle("Ativar atalho de tocha", () -> c.torchEnabled, v -> c.torchEnabled = v));
                rows.add(new Slider("Só colocar se a luz no local for ≤", 0, 15, "", () -> c.torchMaxLight, v -> c.torchMaxLight = v));
                rows.add(new Label("Usa tochas do inventário (normais primeiro, depois de alma) e escolhe a posição válida mais próxima. "
                        + "Sem tocha, nada acontece. Tecla configurável em Controles.", cw));
            }
            case 8 -> {
                rows.add(new Section("Assistente de crafting"));
                rows.add(new ButtonRow("Abrir assistente de crafting", () -> mc.setScreen(new CraftingScreen(this))));
                rows.add(new Label("Consulta receitas, mostra materiais, quantidades no inventário e o que falta. Só consulta: "
                        + "nunca fabrica itens por você.", cw));
            }
            default -> {
                rows.add(new Section("Teclas"));
                for (KeyBinding kb : Keybinds.ALL) rows.add(new KeyRow(kb));
                rows.add(new Label("Clique numa tecla e pressione a nova. ESC cancela; Delete ou Backspace remove a tecla.", cw));
                rows.add(new ButtonRow("Restaurar teclas padrão", () -> {
                    Keybinds.resetAll();
                }));
            }
        }
    }

    private String favoriteName() {
        String f = ConfigManager.get().favoriteFood;
        if (f == null || f.isEmpty()) return "automático";
        var item = Registries.ITEM.get(net.minecraft.util.Identifier.of(f));
        return new ItemStack(item).getName().getString();
    }

    private void toggleFavorite() {
        NivekConfig c = ConfigManager.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            ItemStack hand = mc.player.getMainHandStack();
            if (hand.get(DataComponentTypes.FOOD) != null) {
                c.favoriteFood = Registries.ITEM.getId(hand.getItem()).toString();
                ConfigManager.save();
                return;
            }
        }
        c.favoriteFood = "";
        ConfigManager.save();
    }

    private int totalHeight() {
        int h = 0;
        for (Row r : rows) h += r.height + 2;
        return h;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        Ui.tick();
        ctx.fill(0, 0, width, height, 0xC0000000);
        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, Colors.ACCENT_DARK);
        ctx.fill(px, py, px + pw, py + ph, Colors.PANEL);
        ctx.fill(px, py, px + SIDE_W, py + ph, Colors.SIDEBAR);

        ctx.drawText(textRenderer, Text.literal("NIVEK").formatted(Formatting.RED, Formatting.BOLD), px + 10, py + 8, 0xFFFFFFFF, false);
        ctx.drawText(textRenderer, "UTILITY", px + 10, py + 19, Colors.MUTED, false);

        int h = catH();
        int y0 = py + 34;
        float target = y0 + cat * h;
        if (barY < 0) barY = target;
        barY = Ui.approach(barY, target);
        ctx.fill(px, (int) barY, px + SIDE_W, (int) barY + h, 0xFF1D1D1D);
        ctx.fill(px, (int) barY, px + 3, (int) barY + h, Colors.ACCENT);
        for (int i = 0; i < CATS.length; i++) {
            int iy = y0 + i * h;
            boolean hov = mx >= px && mx < px + SIDE_W && my >= iy && my < iy + h;
            catHover[i] = Ui.approach(catHover[i], hov ? 1f : 0f);
            int col = Colors.lerp(i == cat ? Colors.TEXT : Colors.MUTED, 0xFFFFFFFF, catHover[i]);
            if (h >= 18) ctx.drawItem(new ItemStack(ICONS[i]), px + 8, iy + (h - 16) / 2);
            ctx.drawText(textRenderer, CATS[i], px + (h >= 18 ? 28 : 10), iy + (h - 8) / 2, col, false);
        }

        ctx.drawText(textRenderer, Text.literal(CATS[cat]).formatted(Formatting.BOLD), cx, py + 12, 0xFFFFFFFF, false);
        ctx.fill(cx, py + 24, cx + cw, py + 25, Colors.ACCENT);

        double maxScroll = Math.max(0, totalHeight() - ch);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        ctx.enableScissor(cx, cy, cx + cw, cy + ch);
        int y = cy - (int) scroll;
        for (Row r : rows) {
            if (y + r.height >= cy - 2 && y <= cy + ch + 2) {
                r.render(ctx, textRenderer, cx, y, cw, mx, my);
            } else {
                r.render(ctx, textRenderer, cx, -1000, cw, -1, -1); // mantém posição consistente, fora da tela
            }
            y += r.height + 2;
        }
        ctx.disableScissor();
        if (maxScroll > 0) {
            int barH = Math.max(16, (int) (ch * (ch / (double) totalHeight())));
            int barTop = cy + (int) ((ch - barH) * (scroll / maxScroll));
            ctx.fill(cx + cw + 3, barTop, cx + cw + 5, barTop + barH, Colors.ACCENT);
        }
    }

    private boolean inContent(double mx, double my) {
        return mx >= cx && mx < cx + cw && my >= cy && my < cy + ch;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx >= px && mx < px + SIDE_W && my >= py + 34) {
            int idx = (int) ((my - (py + 34)) / catH());
            if (idx >= 0 && idx < CATS.length && button == 0) {
                setCategory(idx);
                return true;
            }
        }
        for (Row r : rows) if (r.listening()) r.cancelListen();
        if (inContent(mx, my)) {
            for (Row r : rows) {
                if (r.click(mx, my, button)) return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        for (Row r : rows) r.drag(mx, my);
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        for (Row r : rows) r.release();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll -= vertical * 18;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Row r : rows) {
            if (r.listening()) return r.key(keyCode, scanCode);
        }
        if (Keybinds.OPEN_GUI.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        ConfigManager.save();
        if (client != null) client.setScreen(parent);
    }
}
