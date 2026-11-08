package dev.nivek.utility.config;

/** Configurações do cliente (salvas em config/nivekutility.json). */
public class NivekConfig {
    public static class HudEntry {
        public boolean enabled;
        public int x;
        public int y;

        public HudEntry() {
        }

        public HudEntry(boolean enabled, int x, int y) {
            this.enabled = enabled;
            this.x = x;
            this.y = y;
        }
    }

    // Árvores
    public boolean treeEnabled = true;
    public int treeMax = 128;
    public boolean treeBaseOnly = true;

    // Mineração
    public boolean veinEnabled = true;
    public int veinMax = 64;
    /** 0 = sempre, 1 = agachado, 2 = tecla */
    public int veinMode = 2;
    public boolean areaEnabled = false;

    // Agricultura
    public boolean farmClick = true;
    public boolean farmAuto = false;
    public int farmRadius = 3;

    // Tochas
    public boolean torchEnabled = true;
    public int torchMaxLight = 15;

    // Pesca e alimentação
    public boolean fishAlert = true;
    public boolean fishSound = true;
    public boolean lowFoodAlert = true;
    public int lowFoodThreshold = 6;
    public String favoriteFood = "";

    // Inventário
    public boolean showInvButtons = true;

    // HUD
    public boolean hudEnabled = true;
    public int hudScale = 100;
    public HudEntry hudCoords = new HudEntry(true, 4, 4);
    public HudEntry hudBiome = new HudEntry(true, 4, 16);
    public HudEntry hudTool = new HudEntry(true, 4, 28);
    public HudEntry hudArmor = new HudEntry(true, 4, 40);
    public HudEntry hudFood = new HudEntry(false, 4, 52);
    public HudEntry hudHealth = new HudEntry(false, 4, 64);
    public HudEntry hudWaypoint = new HudEntry(true, 4, 76);
    public HudEntry hudFps = new HudEntry(false, 4, 88);
    public HudEntry hudClock = new HudEntry(true, 4, 100);

    // Waypoints
    public String selectedWaypoint = "";

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Corrige valores nulos/fora de faixa (arquivo editado à mão ou de versão antiga). */
    public void sanitize() {
        NivekConfig d = new NivekConfig();
        if (hudCoords == null) hudCoords = d.hudCoords;
        if (hudBiome == null) hudBiome = d.hudBiome;
        if (hudTool == null) hudTool = d.hudTool;
        if (hudArmor == null) hudArmor = d.hudArmor;
        if (hudFood == null) hudFood = d.hudFood;
        if (hudHealth == null) hudHealth = d.hudHealth;
        if (hudWaypoint == null) hudWaypoint = d.hudWaypoint;
        if (hudFps == null) hudFps = d.hudFps;
        if (hudClock == null) hudClock = d.hudClock;
        if (favoriteFood == null) favoriteFood = "";
        if (selectedWaypoint == null) selectedWaypoint = "";
        treeMax = clamp(treeMax, 1, 512);
        veinMax = clamp(veinMax, 1, 512);
        veinMode = clamp(veinMode, 0, 2);
        farmRadius = clamp(farmRadius, 1, 5);
        torchMaxLight = clamp(torchMaxLight, 0, 15);
        lowFoodThreshold = clamp(lowFoodThreshold, 1, 19);
        hudScale = clamp(hudScale, 50, 200);
    }
}
